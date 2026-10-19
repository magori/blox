package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Plays a score in a loop. Each score is rendered once, away from the audio writing, and kept in a
 * cache: one mixed buffer per score, about 2.3 MB for 8 bars at 44 100 Hz. The loop replays that
 * buffer, so it never drifts. The samples go to a sink in slices, the sink paces the loop. Playing
 * another score replaces the one playing: there is never more than one loop, nor more than one
 * open sink. A failure (no audio output, a write refused) stops the music, never the game.
 *
 * A new tempo factor ([setTempoFactor]) is rendered once, in the background, and cached too; the
 * music keeps its sink and switches once that buffer is ready, at the start of the next beat where a
 * note starts in every voice ([switchBeats]), so that no note is cut. The samples are never
 * resampled, which would change the pitch. A tempo rendering that fails leaves the music at its
 * tempo; a rendering under way never holds back a stop, nor the next play.
 */
public class ChiptuneSequencer internal constructor(
    private val render: (Score, Double) -> FloatArray,
    private val sampleRate: Int,
    private val openSink: (sampleRate: Int) -> AudioSink,
    private val scope: CoroutineScope,
    private val renderDispatcher: CoroutineDispatcher,
    private val writeDispatcher: CoroutineDispatcher,
    private val sliceSize: Int,
    private val onFailure: (Exception) -> Unit = {},
) : MusicPlayer {
    /**
     * A sequencer that renders with [synthesizer] on [Dispatchers.Default] and writes to the sinks
     * [openSink] gives on [Dispatchers.IO], for as long as [scope] lives. A failure that stops the music
     * goes to [onFailure], to be logged; it is called on a thread of [Dispatchers.IO], the one the music
     * is written from, not on the main thread.
     */
    public constructor(
        synthesizer: Synthesizer,
        openSink: (sampleRate: Int) -> AudioSink,
        scope: CoroutineScope,
        onFailure: (Exception) -> Unit = {},
    ) : this(
        render = synthesizer::render,
        sampleRate = synthesizer.sampleRate,
        openSink = openSink,
        scope = scope,
        renderDispatcher = Dispatchers.Default,
        writeDispatcher = Dispatchers.IO,
        sliceSize = SLICE_SIZE,
        onFailure = onFailure,
    )

    /** One mixed buffer per score and tempo factor, not one per voice. */
    private val cache = mutableMapOf<Rendering, FloatArray>()
    private var playing: Job? = null

    /** The tempo factor asked for, read by the loop between two slices. */
    @Volatile
    private var requestedTempo = NORMAL_TEMPO

    /**
     * Plays [score] in a loop at the normal tempo, in place of the score playing, if any: the tempo asked
     * for before is forgotten. Call it from the main thread.
     */
    override fun play(score: Score): Playback {
        requestedTempo = NORMAL_TEMPO
        return start(score, from = null)
    }

    /** Plays [score] in a loop from the start, or from where [from] stood, in place of the score playing. */
    private fun start(
        score: Score,
        from: Place?,
    ): Playback {
        val playback = SequencerPlayback(score, from)
        val previous = playing
        val job = scope.launch(writeDispatcher) {
            // The previous loop releases its sink before this one opens its own; the cache is
            // only read and written here, one loop at a time.
            previous?.cancelAndJoin()
            try {
                loop(score, playback, from)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                // No audio output, a rendering or a write that fails: the music stops, the game goes on.
                onFailure(failure)
            }
        }
        playback.job = job
        playing = job
        return playback
    }

    /** Where a loop stood: a [position] in a buffer of [length] samples. */
    private class Place(val position: Int, val length: Int)

    /** A score played by this sequencer, which keeps where its loop stood, to [resume] from there. */
    private inner class SequencerPlayback(private val score: Score, from: Place?) : Playback {
        lateinit var job: Job

        /**
         * Where the loop stood after its last slice written, or, before it wrote anything, where it
         * started from: a playback resumed then stopped at once keeps its place. Read on the main thread.
         */
        @Volatile
        var place: Place? = from

        // Only while this loop is the one playing: a game left after the next one started stops nothing.
        override fun stop() {
            if (playing === job) job.cancel()
        }

        override fun resume(): Playback = start(score, from = place)
    }

    /**
     * Renders [score] at the tempo asked for, or takes it from the cache, then writes it in a loop
     * until cancelled. A new tempo is rendered meanwhile, and played from the next beat once ready.
     */
    private suspend fun loop(
        score: Score,
        playback: SequencerPlayback,
        from: Place?,
    ) {
        val beats = score.voices.first().beats.roundToInt()
        // The beats where no note is cut, then the end of the buffer, where the loop starts again.
        val switches = switchBeats(score) + beats
        var tempo = requestedTempo
        var buffer = cache[Rendering(score, tempo)]
            ?: withContext(renderDispatcher) { render(score, tempo) }.also { cache[Rendering(score, tempo)] = it }
        val sink = openSink(sampleRate)
        // Detached from the loop: a tempo rendering under way never holds back a stop, nor the next play.
        val renderings = CoroutineScope(SupervisorJob() + renderDispatcher)
        try {
            var cursor = LoopCursor(
                buffer,
                from?.let { resumePosition(it.position, it.length, buffer.size, switches, beats) } ?: 0,
            )
            var pending: PendingTempo? = null
            val slice = FloatArray(sliceSize)
            while (true) {
                // A real sink blocks instead of suspending: check for a stop between slices.
                currentCoroutineContext().ensureActive()
                pending = renderings.pendingFor(score, requestedTempo, tempo, pending)
                var written = 0
                val ready = pending?.takeIf { it.buffer.isCompleted }
                // A rendering that failed gives no buffer: the old tempo goes on.
                val readyBuffer = ready?.buffer?.await()
                if (ready != null && readyBuffer != null) {
                    // Switch at the start of the next beat where no note is cut, if it comes within this slice.
                    val grid = BeatGrid(buffer.size, beats)
                    val beat = switches.first { it >= grid.nextBeat(cursor.position) }
                    val beforeBeat = grid.startOf(beat) - cursor.position
                    if (beforeBeat < sliceSize) {
                        cursor.fill(slice, until = beforeBeat)
                        buffer = readyBuffer.also { cache[Rendering(score, ready.factor)] = it }
                        tempo = ready.factor
                        pending = null
                        cursor = LoopCursor(buffer, BeatGrid(buffer.size, beats).startOf(beat % beats))
                        written = beforeBeat
                    }
                }
                cursor = cursor.fill(slice, from = written)
                playback.place = Place(cursor.position, buffer.size)
                sink.write(slice)
            }
        } finally {
            renderings.cancel()
            sink.release()
        }
    }

    /**
     * The rendering of the tempo [wanted], when it is not the [playing] one: [pending] if it is for
     * that tempo already, else the cached buffer or a new rendering. A rendering that is no longer
     * wanted is cancelled: of quick changes, only the last one plays.
     */
    private fun CoroutineScope.pendingFor(
        score: Score,
        wanted: Double,
        playing: Double,
        pending: PendingTempo?,
    ): PendingTempo? {
        if (pending?.factor == wanted) return pending
        pending?.buffer?.cancel()
        if (wanted == playing) return null
        val buffer = cache[Rendering(score, wanted)]?.let { CompletableDeferred<FloatArray?>(it) }
            ?: async(renderDispatcher) { renderOrNull(score, wanted) }
        return PendingTempo(wanted, buffer)
    }

    /** A tempo asked for, and its buffer, rendered or being rendered; null if its rendering failed. */
    private class PendingTempo(val factor: Double, val buffer: Deferred<FloatArray?>)

    /**
     * [score] rendered at [tempoFactor], or null when the rendering fails: a new tempo that cannot be
     * rendered leaves the music at its tempo. Only the tempo renderings are caught; a failure of the
     * sink still stops the music.
     */
    private fun renderOrNull(
        score: Score,
        tempoFactor: Double,
    ): FloatArray? = try {
        render(score, tempoFactor)
    } catch (failure: Exception) {
        null
    }

    /**
     * Stops the score playing and releases its sink; does nothing if none is playing. Call it from
     * the main thread.
     */
    public fun stop() {
        // The job is kept: the next play waits for it to have released its sink.
        playing?.cancel()
    }

    /**
     * Sets the tempo factor of the music: 1.0 is the tempo of the score, the caller chooses another
     * one (the stack danger). Kept for the next resume when nothing is playing; the next [play] starts
     * at 1.0 again. Call it from the main thread.
     */
    override fun setTempoFactor(factor: Double) {
        require(factor > 0 && factor.isFinite()) { "The tempo factor is positive and finite, was $factor" }
        requestedTempo = factor
    }

    /** What a buffer is rendered from: a score, at a tempo factor. */
    private data class Rendering(val score: Score, val tempoFactor: Double)

    private companion object {
        const val NORMAL_TEMPO = 1.0
    }
}

/** Samples per slice: about 23 ms at 44 100 Hz, short enough to stop quickly, long enough to keep the sink fed. */
internal const val SLICE_SIZE: Int = 1_024

/**
 * Where a loop stopped at [position] in a buffer of [length] samples resumes in a buffer of [newLength]
 * samples of [beats] beats: the same place in the score, `position × newLength / length` rounded and
 * brought back into the buffer, then back to the start of the beat of [switchBeats] it is in, so that
 * no note is cut. Computed on [Long].
 */
internal fun resumePosition(
    position: Int,
    length: Int,
    newLength: Int,
    switchBeats: List<Int>,
    beats: Int,
): Int {
    val scaled = ((2L * position * newLength + length) / (2L * length) % newLength).toInt()
    val grid = BeatGrid(newLength, beats)
    return grid.startOf(switchBeats.last { grid.startOf(it) <= scaled })
}
