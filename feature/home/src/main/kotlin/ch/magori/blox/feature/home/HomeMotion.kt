package ch.magori.blox.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.game.model.Level

/**
 * What moves on the home (P5.77), counted in ticks: the [replay] of the preview, at the level chosen, and the [halo] of
 * Jouer, which counts from the last touch. The screen gives it its ticks ([advance]) and reads [preview] and [halo]
 * only while drawing: nothing recomposes as they move. Both are states that change only when what they show changes,
 * the T a row lower, its lock, a step of a pulse: between, nothing is drawn again.
 */
@Stable
internal class HomeMotion(level: Level) {
    /** Where the preview is, tick by tick; not a state: what it shows is [preview]. */
    var replay: PreviewReplay = PreviewReplay(level)
        private set

    /** What the preview shows: its board, its T and its points. */
    var preview: PreviewFrame by mutableStateOf(replay.frame)
        private set

    private var sinceTouch = 0

    /** The halo of Jouer: 0 while there is none, how far its pulse has gone otherwise ([playHalo]). */
    var halo: Float by mutableFloatStateOf(playHalo(sinceTouch))
        private set

    /** [ticks] go by, the [level] chosen: a new level starts the preview again. */
    fun advance(
        ticks: Int,
        level: Level,
    ) {
        replay = replay.withLevel(level).advancedBy(ticks)
        preview = replay.frame
        sinceTouch += ticks
        halo = playHalo(sinceTouch)
    }

    /** The player touched the home: the halo waits 3 seconds again. */
    fun touched() {
        sinceTouch = 0
        halo = playHalo(sinceTouch)
    }
}

/**
 * The [HomeMotion] of the home at [level], given the ticks of the frames of the screen (ADR 0012) while it [plays]: in
 * reduced effects, it stands still.
 */
@Composable
internal fun rememberHomeMotion(
    level: Level,
    plays: Boolean,
): HomeMotion {
    val motion = remember { HomeMotion(level) }
    val currentLevel by rememberUpdatedState(level)
    if (plays) {
        LaunchedEffect(motion) {
            val frames = FrameTicks()
            while (true) withFrameNanos { motion.advance(frames.advance(it), currentLevel) }
        }
    }
    return motion
}
