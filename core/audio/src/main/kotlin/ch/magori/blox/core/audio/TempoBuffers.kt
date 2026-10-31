package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.SoundStyle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

/**
 * The buffers of one [score] in one [style], one mixed buffer per tempo factor, and its renderings under way, on
 * [renderDispatcher]. It lives as long as its score is one the playback played last played or was switched to:
 * its renderings survive a stop, so that a resume finds them done, and a switch, so that switching back finds
 * them ready; [abandon] cancels them once another score plays. Used by one loop at a
 * time, from the thread that writes the music. Its renderings are children of [parent], the job of the
 * sequencer: they end with the sequencer too.
 *
 * A tempo is either prepared ([prepare]: rendered ahead, kept when no longer asked for) or asked for
 * ([request]: cancelled once given up, so that of quick changes only the last one is rendered). A
 * rendering that fails leaves no buffer; it is not tried again before it is given up.
 */
internal class TempoBuffers(
    val score: Score,
    val style: SoundStyle,
    private val render: (Score, Double, SoundStyle) -> FloatArray,
    renderDispatcher: CoroutineDispatcher,
    parent: Job?,
) {
    private val scope = CoroutineScope(SupervisorJob(parent) + renderDispatcher)
    private val buffers = mutableMapOf<Double, FloatArray>()
    private val renderings = mutableMapOf<Double, Deferred<FloatArray>>()
    private val prepared = mutableSetOf<Double>()

    /**
     * The buffer at [factor]: from the cache, else once its rendering under way, or a new one, is done.
     * Throws when that rendering fails.
     */
    suspend fun bufferOf(factor: Double): FloatArray {
        buffers[factor]?.let { return it }
        val rendering = renderings[factor]?.takeUnless { it.isCancelled } ?: start(factor)
        return rendering.await().also {
            buffers[factor] = it
            renderings -= factor
        }
    }

    /** The buffer at [factor] if it is ready, once [harvest]ed; null while it renders, or if it failed. */
    fun ready(factor: Double): FloatArray? = buffers[factor]

    /** Renders [factor] in the background, unless it is ready or under way, and keeps it when given up. */
    fun prepare(factor: Double) {
        prepared += factor
        request(factor)
    }

    /** Renders [factor] in the background, unless it is ready, under way, or failed. */
    fun request(factor: Double) {
        if (factor !in buffers && factor !in renderings) start(factor)
    }

    /**
     * [factor] is no longer asked for: its rendering is cancelled, unless it is prepared and has not
     * failed. A rendering failed is forgotten, to be tried again once asked for anew.
     */
    fun giveUp(factor: Double) {
        val rendering = renderings[factor] ?: return
        if (factor in prepared && !rendering.isCancelled) return
        rendering.cancel()
        renderings -= factor
    }

    /** Moves the renderings done into the buffers; a failed one is left as it is. */
    suspend fun harvest() {
        val done = renderings.entries.iterator()
        while (done.hasNext()) {
            val (factor, rendering) = done.next()
            if (rendering.isCompleted && !rendering.isCancelled) {
                buffers[factor] = rendering.await()
                done.remove()
            }
        }
    }

    /**
     * Releases the buffers of the tempos other than [playing], [asked] and the tempos prepared: a tempo left, as a
     * level of a live game, is not kept for the whole life of the score, and is rendered anew if asked for again.
     */
    fun releaseAllBut(
        playing: Double,
        asked: Double,
    ) {
        val factors = buffers.keys.iterator()
        while (factors.hasNext()) {
            val factor = factors.next()
            if (factor != playing && factor != asked && factor !in prepared) factors.remove()
        }
    }

    /** Cancels the renderings under way: another score plays. */
    fun abandon() {
        scope.cancel()
    }

    private fun start(factor: Double): Deferred<FloatArray> =
        scope.async { render(score, factor, style) }.also { renderings[factor] = it }
}
