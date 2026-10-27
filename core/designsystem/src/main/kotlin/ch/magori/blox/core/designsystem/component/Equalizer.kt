package ch.magori.blox.core.designsystem.component

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** A bar of the equalizer at its full height, its corners, and the gap between two bars. */
private val BarWidth = 3.dp
private val BarHeight = 14.dp
private val BarCorner = 2.dp
private val BarGap = 2.dp

/** The lowest part of its height a bar of the equalizer falls to. */
private const val LOWEST_PART = 0.35f

/** The ticks a bar takes to rise from [LOWEST_PART] to its full height (0,5 s), and as many to fall back. */
private const val RISE_TICKS = 30

/** The bars of the equalizer, each this many ticks ahead of the one before it. */
private const val BARS = 3
private const val BAR_LEAD_TICKS = 10

/**
 * The equalizer of a music being listened to (`EQUALIZER`, docs/design-system/pictogrammes.md): three bars of 3 × 14
 * dp in `accent`, 2 dp apart, standing on their foot, that rise and fall on the frames of the screen (ADR 0012). Only
 * its drawing follows them: it does not compose again while it moves (ADR 0024). It stands still in reduced effects
 * and in Original, as rising stairs. Decorative: TalkBack leaves it out, the listen button beside it already says that
 * the music plays.
 */
@Composable
fun Equalizer(modifier: Modifier = Modifier) {
    val accent = BloxTheme.palette.accent
    val moves = !BloxTheme.effects.reduced && BloxTheme.kind != BloxThemeKind.ORIGINAL
    val motion = remember(moves) { EqualizerMotion() }
    if (moves) LaunchedEffect(motion) { motion.run() }
    Spacer(
        modifier = modifier
            .size(BarWidth * BARS + BarGap * (BARS - 1), BarHeight)
            .semantics { hideFromAccessibility() }
            .drawBehind { drawBars(equalizerParts(motion.tick), accent) },
    )
}

/** The ticks the equalizer has moved for, counted on the frames of the screen. */
@Stable
private class EqualizerMotion {
    var tick by mutableIntStateOf(0)
        private set

    /**
     * Counts the ticks, frame by frame, as long as the equalizer is shown: an infinite animation, that the tests stop
     * unless they move the clock by hand.
     */
    suspend fun run() {
        val frames = FrameTicks()
        while (true) {
            withInfiniteAnimationFrameNanos { tick = (tick + frames.advance(it)) % CYCLE_TICKS }
        }
    }

    private companion object {
        /** A whole rise and fall: the bars then stand as they did at the start. */
        const val CYCLE_TICKS = 2 * RISE_TICKS
    }
}

/** The bars, each [parts] of the height of the box, side by side from the left, standing on the foot of the box. */
private fun DrawScope.drawBars(
    parts: List<Float>,
    color: Color,
) {
    val width = BarWidth.toPx()
    val gap = BarGap.toPx()
    val corner = BarCorner.toPx().coerceAtMost(width / 2)
    parts.forEachIndexed { bar, part ->
        val height = size.height * part
        drawRoundRect(
            color = color,
            topLeft = Offset(bar * (width + gap), size.height - height),
            size = Size(width, height),
            cornerRadius = CornerRadius(corner),
        )
    }
}

/**
 * The part of their full height the three bars of the equalizer reach at [tick]: each from 35 % up to 100 % in 30
 * ticks and back down in as many, each 10 ticks ahead of the one before it, so that they start as rising stairs.
 */
internal fun equalizerParts(tick: Int): List<Float> = List(BARS) { bar ->
    val phase = (tick + bar * BAR_LEAD_TICKS) % (2 * RISE_TICKS)
    val rise = if (phase <= RISE_TICKS) phase else 2 * RISE_TICKS - phase
    LOWEST_PART + (1f - LOWEST_PART) * rise / RISE_TICKS
}
