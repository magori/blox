package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * The [SoundEffectPlayer] of the device: each effect is rendered once, by [render], into its own track, opened by
 * [openTrack] when it is warmed up or at its first play, and kept, so that the next plays only replay it, with as
 * little latency as the output allows. An effect plays over another one, on its own track, without cutting it;
 * played again while it still plays, it starts again from its start.
 *
 * [warmUp] and [play] return at once: the rendering, the opening and the play run on [scope], whose dispatcher runs
 * one of them at a time (the tracks are kept without a lock), so that the main thread never waits for the audio
 * output. A failure, of the rendering, of an output that cannot open or of a track that fails to play, plays nothing
 * and goes to [onFailure], called on [scope], never to the caller: the game goes on in silence. A track that failed
 * is released and forgotten; its effect is not tried again for [RETRY_DELAY], measured on [timeSource], so that an
 * output that keeps failing neither loads the processor nor fills the log at every rotation. Its rendering is kept.
 *
 * The tracks are kept as long as the player, which the application keeps as long as its process.
 */
public class SoundEffectTracks(
    private val render: (SoundEffect) -> FloatArray,
    private val openTrack: (FloatArray) -> EffectTrack,
    private val scope: CoroutineScope,
    private val onFailure: (Throwable) -> Unit,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) : SoundEffectPlayer {
    /** The samples of each effect rendered. */
    private val renderings = mutableMapOf<SoundEffect, FloatArray>()

    /** The track of each effect opened. */
    private val tracks = mutableMapOf<SoundEffect, EffectTrack>()

    /** When each effect that failed may be tried again. */
    private val retries = mutableMapOf<SoundEffect, ComparableTimeMark>()

    override fun warmUp(effects: List<SoundEffect>) {
        scope.launch { effects.forEach(::trackOf) }
    }

    override fun play(effect: SoundEffect) {
        scope.launch { trackOf(effect)?.let { play(effect, it) } }
    }

    /**
     * The track of [effect], rendered and opened if it is not yet; null while it may not be tried again, or if it
     * fails now.
     */
    private fun trackOf(effect: SoundEffect): EffectTrack? {
        tracks[effect]?.let { return it }
        val retry = retries[effect]
        if (retry != null && timeSource.markNow() < retry) return null
        return try {
            openTrack(renderings.getOrPut(effect) { render(effect) }).also { tracks[effect] = it }
        } catch (failure: RuntimeException) {
            failed(effect, failure)
            null
        }
    }

    private fun play(
        effect: SoundEffect,
        track: EffectTrack,
    ) {
        try {
            track.play()
        } catch (failure: RuntimeException) {
            tracks.remove(effect)
            failed(effect, failure)
            release(track)
        }
    }

    /** [effect] failed with [failure]: reported, and not tried again for [RETRY_DELAY]. */
    private fun failed(
        effect: SoundEffect,
        failure: RuntimeException,
    ) {
        retries[effect] = timeSource.markNow() + RETRY_DELAY
        onFailure(failure)
    }

    /** Releases [track], which failed; a release that fails too is reported, and the track forgotten all the same. */
    private fun release(track: EffectTrack) {
        try {
            track.release()
        } catch (failure: RuntimeException) {
            onFailure(failure)
        }
    }

    private companion object {
        /** How long an effect that failed waits before it is tried again: a few rotations, not every one. */
        val RETRY_DELAY = 5.seconds
    }
}
