package ch.magori.blox.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.game.model.Level

/**
 * What moves on the home (P5.77), counted in ticks: the [replay] of the preview, at the level chosen, and the [halo] of
 * Jouer, which counts from the last touch. The screen gives it its ticks ([advance]) and reads it only while drawing:
 * nothing recomposes as it moves.
 */
@Stable
internal class HomeMotion(level: Level) {
    var replay: PreviewReplay by mutableStateOf(PreviewReplay(level))
        private set

    private var sinceTouch by mutableIntStateOf(0)

    /** The halo of Jouer: 0 while there is none, how far its pulse has gone otherwise ([playHalo]). */
    val halo: Float get() = playHalo(sinceTouch)

    /** [ticks] go by, the [level] chosen: a new level starts the preview again. */
    fun advance(
        ticks: Int,
        level: Level,
    ) {
        replay = replay.withLevel(level).advancedBy(ticks)
        sinceTouch += ticks
    }

    /** The player touched the home: the halo waits 3 seconds again. */
    fun touched() {
        sinceTouch = 0
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
