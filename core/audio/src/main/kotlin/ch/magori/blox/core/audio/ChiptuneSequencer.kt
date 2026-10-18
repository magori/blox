package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays a score in a loop. Each score is rendered once, away from the audio writing, and kept in a
 * cache: one mixed buffer per score, about 2.3 MB for 8 bars at 44 100 Hz. The loop replays that
 * buffer, so it never drifts. The samples go to a sink in slices, the sink paces the loop. Playing
 * another score replaces the one playing: there is never more than one loop, nor more than one
 * open sink. A failure (no audio output, a write refused) stops the music, never the game.
 */
public class ChiptuneSequencer internal constructor(
    private val render: (Score, Double) -> FloatArray,
    private val sampleRate: Int,
    private val openSink: (sampleRate: Int) -> AudioSink,
    private val scope: CoroutineScope,
    private val renderDispatcher: CoroutineDispatcher,
    private val writeDispatcher: CoroutineDispatcher,
    private val sliceSize: Int,
) {
    /**
     * A sequencer that renders with [synthesizer] on [Dispatchers.Default] and writes to the sinks
     * [openSink] gives on [Dispatchers.IO], for as long as [scope] lives.
     */
    public constructor(
        synthesizer: Synthesizer,
        openSink: (sampleRate: Int) -> AudioSink,
        scope: CoroutineScope,
    ) : this(
        render = synthesizer::render,
        sampleRate = synthesizer.sampleRate,
        openSink = openSink,
        scope = scope,
        renderDispatcher = Dispatchers.Default,
        writeDispatcher = Dispatchers.IO,
        sliceSize = SLICE_SIZE,
    )

    /** One mixed buffer per score and tempo factor, not one per voice. */
    private val cache = mutableMapOf<Rendering, FloatArray>()
    private var playing: Job? = null

    /** Plays [score] in a loop, in place of the score playing, if any. Call it from the main thread. */
    public fun play(score: Score) {
        val previous = playing
        playing = scope.launch(writeDispatcher) {
            // The previous loop releases its sink before this one opens its own; the cache is
            // only read and written here, one loop at a time.
            previous?.cancelAndJoin()
            try {
                loop(score)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                // No audio output, a rendering or a write that fails: the music stops, the game goes on.
            }
        }
    }

    /** Renders [score] or takes it from the cache, then writes it in a loop until cancelled. */
    private suspend fun loop(score: Score) {
        val rendering = Rendering(score, tempoFactor = NORMAL_TEMPO)
        val buffer = cache[rendering]
            ?: withContext(renderDispatcher) { render(score, rendering.tempoFactor) }.also { cache[rendering] = it }
        val sink = openSink(sampleRate)
        try {
            var cursor = LoopCursor(buffer)
            val slice = FloatArray(sliceSize)
            while (true) {
                // A real sink blocks instead of suspending: check for a stop between slices.
                currentCoroutineContext().ensureActive()
                cursor = cursor.fill(slice)
                sink.write(slice)
            }
        } finally {
            sink.release()
        }
    }

    /**
     * Stops the score playing and releases its sink; does nothing if none is playing. Call it from
     * the main thread.
     */
    public fun stop() {
        // The job is kept: the next play waits for it to have released its sink.
        playing?.cancel()
    }

    /** What a buffer is rendered from: a score, at a tempo factor (1.0 until the stack danger, P4.5). */
    private data class Rendering(val score: Score, val tempoFactor: Double)

    private companion object {
        const val NORMAL_TEMPO = 1.0
    }
}

/** Samples per slice: about 23 ms at 44 100 Hz, short enough to stop quickly, long enough to keep the sink fed. */
internal const val SLICE_SIZE: Int = 1_024
