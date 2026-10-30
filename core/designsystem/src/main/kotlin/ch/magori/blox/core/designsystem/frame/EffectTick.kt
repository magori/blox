package ch.magori.blox.core.designsystem.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * The tick of an effect a screen shows (ADR 0024), counted on the frames of the screen ([FrameTicks]): from [start]
 * each time [key] changes, the effect starting over, until [length], when it is over. Read in the drawing alone: the
 * frames that count it recompose nothing. A [start] at [length] or beyond shows the effect over, as one frozen for a
 * preview, or one played already. Shared by the screens that play effects outside a game: the Locksmith and the
 * musical garden.
 */
@Composable
fun rememberEffectTick(
    key: Any?,
    start: Int,
    length: Int,
): () -> Int {
    // Set as soon as the key changes, so that the first frame of an effect never shows the tick of the one before.
    val ticks = remember(key, start) { mutableIntStateOf(start) }
    LaunchedEffect(ticks, length) {
        val frames = FrameTicks()
        while (ticks.intValue < length) {
            withFrameNanos { ticks.intValue += frames.advance(it) }
        }
    }
    return remember(ticks) { { ticks.intValue } }
}

/**
 * The tick of the effect of the action numbered [move] (null without an effect), keyed by [key], counted on the frames
 * of the screen from 0 until [length]; [frozen] at a tick for the previews, never counted then. The effect of the
 * action shown first, as on coming back to the screen, is over already: it is not played again. Shared by the
 * Locksmith and the musical garden, whose actions are numbered.
 */
@Composable
fun rememberEffectTick(
    key: Any?,
    move: Int?,
    frozen: Int?,
    length: Int,
): () -> Int {
    val first = remember { move }
    return rememberEffectTick(
        key = key,
        start = frozen ?: if (move == null || move == first) Int.MAX_VALUE else 0,
        length = if (frozen != null) 0 else length,
    )
}
