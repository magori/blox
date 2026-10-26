package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The tag of the bars of a [LevelGauge], read by the tests only. */
internal const val LEVEL_GAUGE_BARS_TAG = "level_gauge_bars"

/** The width of a bar of the gauge, and the heights of the first and of the last. */
private val BarWidth = 8.dp
private val LowestBar = 8.dp
private val HighestBar = 26.dp

/**
 * The gauge of a level (docs/design-system/components/Choice): a bar a level of [levels], up to [level] in `accent`,
 * between − and +, − disabled at the lowest level and + at the highest; a touch or a drag on the bars gives the level
 * of the nearest one. TalkBack reads one slider, [description] and
 * the level, and moves it one level at a time; − and +, there for a finger, are left out of it. [onLevelChange] is
 * given the level asked for, always one of [levels].
 */
@Composable
fun LevelGauge(
    level: Int,
    levels: IntRange,
    onLevelChange: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val value = SteppedValue(level, levels)
    val latestValue by rememberUpdatedState(value)
    val latestChange by rememberUpdatedState(onLevelChange)
    val change = remember { { next: SteppedValue -> latestChange(next.value) } }
    Row(
        modifier = modifier.stepSemantics(value, description, state = level.toString(), onValueChange = change),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            description = stringResource(R.string.core_designsystem_stepper_decrease),
            enabled = value.canDecrease,
            onClick = { change(value.decreased()) },
            glyph = Glyphs::minus,
        )
        GaugeBars(value, Modifier.stepTouch(BarWidth / 2, LocalLayoutDirection.current, { latestValue }, change))
        StepButton(
            description = stringResource(R.string.core_designsystem_stepper_increase),
            enabled = value.canIncrease,
            onClick = { change(value.increased()) },
            glyph = Glyphs::plus,
        )
    }
}

/** A button − or + of a gauge, drawn by [glyph]: [description] is for the tests, TalkBack leaves the button out. */
@Composable
private fun StepButton(
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    glyph: (DrawScope, Color) -> Unit,
) {
    GlyphButton(
        description = description,
        onClick = onClick,
        modifier = Modifier.semantics { hideFromAccessibility() },
        enabled = enabled,
        glyph = { ink -> glyph(this, ink) },
    )
}

/**
 * The bars of [value], side by side, [BloxSpaces.space1] apart, standing on the same line, in a box as high as a touch
 * target.
 */
@Composable
private fun GaugeBars(
    value: SteppedValue,
    modifier: Modifier = Modifier,
) {
    val bars = gaugeBars(value, BloxTheme.palette)
    Row(
        modifier = modifier.height(BloxSizes.minTouchTarget).testTag(LEVEL_GAUGE_BARS_TAG),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bars.forEach { bar ->
            Spacer(
                Modifier.size(bar.width, HighestBar).wrapContentHeight(Alignment.Bottom).height(bar.height)
                    .background(bar.color),
            )
        }
    }
}

/** A bar of the gauge: [width] by [height], in [color]. */
@Immutable
internal data class GaugeBar(val width: Dp, val height: Dp, val color: Color)

/**
 * The bars of a gauge at [value], one a step, higher and higher from [LowestBar] to [HighestBar], in the
 * [stepColors] of [palette]: reached up to the value, beyond past it.
 */
internal fun gaugeBars(
    value: SteppedValue,
    palette: BloxPalette,
): List<GaugeBar> {
    val colors = stepColors(palette)
    return value.steps.map { step ->
        val bar = value.copy(value = step)
        GaugeBar(
            width = BarWidth,
            height = lerp(LowestBar, HighestBar, bar.fraction),
            color = if (bar.index <= value.index) colors.reached else colors.beyond,
        )
    }
}
