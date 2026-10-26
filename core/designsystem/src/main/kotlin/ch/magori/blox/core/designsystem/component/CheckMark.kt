package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSpaces

/** A check drawn as one stroke through its three [points], start, bottom and end, [width] wide. */
internal data class CheckStroke(val points: List<Offset>, val width: Float)

/** The check of a selected choice in a square of [side]: down to the right, then up, inside the square. */
internal fun checkStroke(side: Float): CheckStroke = CheckStroke(
    points = listOf(
        Offset(side * 0.15f, side * 0.5f),
        Offset(side * 0.4f, side * 0.75f),
        Offset(side * 0.85f, side * 0.25f),
    ),
    width = side / 6,
)

/** The room of a choice, along its width: [start], the [check] and its [gap] before the label, then [end]. */
internal data class ChoiceInsets(val start: Dp, val check: Dp, val gap: Dp, val end: Dp)

/**
 * The room of a choice, [selected] or not, whose label is [label] high (its font size). Only a selected choice makes
 * room for its check and its gap, before its label: a row of choices, which holds one selected choice, widens by one
 * check at most and does not wrap sooner, and unselected labels keep their size in a large font. The check is as
 * large as the label, so it grows with the font, up to [MAX_CHECK]: on a choice of two lines, it leaves room to the
 * name. It stands a quarter of the label away from it, [MIN_CHECK_GAP] at least.
 */
internal fun choiceInsets(
    selected: Boolean,
    label: Dp,
): ChoiceInsets = if (selected) {
    val gap = (label * CHECK_GAP_EM).coerceAtLeast(MIN_CHECK_GAP)
    ChoiceInsets(start = CHOICE_PADDING, check = label.coerceAtMost(MAX_CHECK), gap = gap, end = CHOICE_PADDING)
} else {
    ChoiceInsets(start = CHOICE_PADDING, check = 0.dp, gap = 0.dp, end = CHOICE_PADDING)
}

/** The room between the sides of a segment and what it holds, its label or its check: `space-3`. */
internal val CHOICE_PADDING = BloxSpaces.space3

/** The largest check, in a large font. */
private val MAX_CHECK = 24.dp

/** The room between a check and its label, as a part of the label: a quarter. */
private const val CHECK_GAP_EM = 0.25f

/** The least room between a check and its label. */
private val MIN_CHECK_GAP = 4.dp

/**
 * A check, in [color], as wide as the smaller side of its box: the cue of a retained choice (ChoiceGroup), shared by
 * what bears one. Decorative: TalkBack already reads the state of what bears it, so it has no description. Its
 * ends are square, so that, with half its width around each point, it stays within its square.
 */
@Composable
fun CheckMark(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = checkStroke(size.minDimension)
        val path = Path().apply {
            val (start, bottom, end) = stroke.points
            moveTo(start.x, start.y)
            lineTo(bottom.x, bottom.y)
            lineTo(end.x, end.y)
        }
        drawPath(path, color, style = Stroke(width = stroke.width, cap = StrokeCap.Square, join = StrokeJoin.Miter))
    }
}
