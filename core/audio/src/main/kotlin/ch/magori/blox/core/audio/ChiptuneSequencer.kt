package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

/**
 * Plays a score in a loop. Each score is rendered once, away from the audio writing, and kept in a
 * cache ([TempoBuffers]), one mixed buffer per tempo factor, about 2.3 MB for 8 bars at 44 100 Hz. The
 * cache only holds the score played last, with its renderings under way, which a stop does not cancel:
 * playing another score abandons them. The loop replays that buffer, so it never drifts. The samples go
 * to a sink in slices, the sink paces the loop. Playing another score replaces the one playing: there is
 * never more than one loop, nor more than one open sink. A sink that dies ([DeadSinkException]) is
 * released and another one opened ([ReopeningSink]): the music goes on where it stood. Any other failure
 * (no audio output, a write refused, a sink that keeps dying) stops the music, never the game.
 *
 * As soon as a score plays, its normal tempo and the tempos prepared for it ([prepareTempo], the danger
 * tempo of a game) are rendered in the background, if not cached yet, so that a change of tempo later
 * finds its buffer ready. A new tempo factor ([setTempoFactor]) is otherwise rendered once, in the
 * background, and cached too; the music keeps its sink and switches once that buffer is ready, at the
 * start of the next beat where a note starts in every voice ([switchBeats]), so that no note is cut.
 * The samples are never resampled, which would change the pitch. A tempo rendering that fails leaves
 * the music at its tempo; a rendering under way never holds back a stop, nor the next play.
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

    /** Held by the loop playing: never two loops, nor two open sinks, at a time. */
    private val sinkLock = Mutex()

    /**
     * The buffers and renderings of the score played last; another score abandons them. Only used by the
     * loop, under [sinkLock].
     */
    private var cache: TempoBuffers? = null
    private var playing: Job? = null

    /** The tempo factor asked for, read by the loop between two slices. */
    @Volatile
    private var requestedTempo = NORMAL_TEMPO

    /** The tempo factors to render ahead for the score played last, read by the loop between two slices. */
    @Volatile
    private var preparedTempos: Set<Double> = emptySet()

    /**
     * Plays [score] in a loop at the normal tempo, in place of the score playing, if any: the tempo asked
     * for before is forgotten, and so are the tempos prepared. Call it from the main thread.
     */
    override fun play(score: Score): Playback {
        requestedTempo = NORMAL_TEMPO
        preparedTempos = emptySet()
        return start(score, from = null)
    }

    /**
     * Renders the score played last at [factor] in the background, as soon as it plays, so that switching
     * to that tempo later renders nothing. Kept for a resume, forgotten by the next [play]. Call it from
     * the main thread.
     */
    override fun prepareTempo(factor: Double) {
        requireValidTempo(factor)
        preparedTempos = preparedTempos + factor
    }

    /** Plays [score] in a loop from the start, or from where [from] stood, in place of the score playing. */
    private fun start(
        score: Score,
        from: Place?,
    ): Playback {
        val playback = SequencerPlayback(score, from)
        // Stopped at once: the loop playing writes no more slice, nor sees the tempos of the next score.
        playing?.cancel()
        val job = scope.launch(writeDispatcher) {
            // One loop at a time: this one opens its sink once the one before has released its own, in
            // the order of the plays; a loop stopped while it waits never gets it. The cache is only used
            // under this lock.
            sinkLock.withLock {
                try {
                    loop(score, playback, from)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    // No audio output, a rendering or a write that fails: the music stops, the game goes on.
                    onFailure(failure)
                }
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
     * Renders [score] at the tempo asked for, or takes it from the cache, then writes it in a loop until
     * cancelled. Its normal tempo and the tempos prepared are rendered meanwhile, and so is a new tempo
     * asked for, which plays from the next beat once ready.
     */
    private suspend fun loop(
        score: Score,
        playback: SequencerPlayback,
        from: Place?,
    ) {
        val beats = score.voices.first().beats.roundToInt()
        // The beats where no note is cut, then the end of the buffer, where the loop starts again.
        val switches = switchBeats(score) + beats
        val buffers = buffersFor(score)
        var tempo = requestedTempo
        var buffer = buffers.bufferOf(tempo)
        // An output that dies (a restart of the audio server, an output gone) is reopened.
        val sink = ReopeningSink { openSink(sampleRate) }
        // The tempo asked for last, rendered in the background while it is not the one playing.
        var asked = tempo
        try {
            var cursor = LoopCursor(
                buffer,
                from?.let { resumePosition(it.position, it.length, buffer.size, switches, beats) } ?: 0,
            )
            val slice = FloatArray(sliceSize)
            while (true) {
                // A real sink blocks instead of suspending: check for a stop between slices.
                currentCoroutineContext().ensureActive()
                buffers.prepare(NORMAL_TEMPO)
                preparedTempos.forEach(buffers::prepare)
                buffers.harvest()
                val wanted = requestedTempo
                if (wanted != asked) {
                    // Of quick changes, only the last one is rendered.
                    if (asked != tempo) buffers.giveUp(asked)
                    asked = wanted
                    if (asked != tempo) buffers.request(asked)
                }
                var written = 0
                // A rendering that failed gives no buffer: the old tempo goes on.
                val ready = if (asked != tempo) buffers.ready(asked) else null
                if (ready != null) {
                    // Switch at the start of the next beat where no note is cut, if it comes within this slice.
                    val grid = BeatGrid(buffer.size, beats)
                    val beat = switches.first { it >= grid.nextBeat(cursor.position) }
                    val beforeBeat = grid.startOf(beat) - cursor.position
                    if (beforeBeat < sliceSize) {
                        cursor.fill(slice, until = beforeBeat)
                        buffer = ready
                        tempo = asked
                        cursor = LoopCursor(buffer, BeatGrid(buffer.size, beats).startOf(beat % beats))
                        written = beforeBeat
                    }
                }
                cursor = cursor.fill(slice, from = written)
                playback.place = Place(cursor.position, buffer.size)
                sink.write(slice)
            }
        } finally {
            // A tempo asked for, not prepared, is not rendered for nothing; a tempo prepared goes on rendering.
            if (asked != tempo) buffers.giveUp(asked)
            sink.release()
        }
    }

    /** The buffers of [score]: those of the score played last, or new ones, the others abandoned. */
    private fun buffersFor(score: Score): TempoBuffers {
        cache?.takeIf { it.score == score }?.let { return it }
        cache?.abandon()
        return TempoBuffers(score, render, renderDispatcher, parent = scope.coroutineContext[Job]).also { cache = it }
    }

    /**
     * Stops the score playing and releases its sink; does nothing if none is playing. Call it from
     * the main thread.
     */
    public fun stop() {
        playing?.cancel()
    }

    /**
     * Sets the tempo factor of the music: 1.0 is the tempo of the score, the caller chooses another
     * one (the stack danger). Kept for the next resume when nothing is playing; the next [play] starts
     * at 1.0 again. Call it from the main thread.
     */
    override fun setTempoFactor(factor: Double) {
        requireValidTempo(factor)
        requestedTempo = factor
    }

    private fun requireValidTempo(factor: Double) {
        require(factor > 0 && factor.isFinite()) { "The tempo factor is positive and finite, was $factor" }
    }

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
