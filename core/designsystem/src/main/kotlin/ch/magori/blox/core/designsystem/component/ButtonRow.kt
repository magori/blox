package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sign

/*
 * Whether buttons framed as panels ([FramedBox]) hold side by side, their labels whole, or have to stand one above the
 * other: the toolbar of the editor, the tiles of the home. Counted in whole pixels, as the layout counts them.
 */

/**
 * The width, in whole pixels, of the widest word of [text] in [style], on one line: a label going to the line between
 * its words needs at least this room. The width of its line, as a text checks that it fits, not of its paragraph, a
 * pixel wider at times (« Allonger » in the pixel font at 8 sp: 178 px, in a paragraph of 179 px).
 */
fun TextMeasurer.widestWord(
    text: String,
    style: TextStyle,
): Int = text.split(' ').maxOf { word ->
    val layout = measure(word, style, softWrap = false, maxLines = 1)
    ceil(layout.getLineRight(0) - layout.getLineLeft(0)).toInt()
}

/**
 * The widths, in pixels, that a `Row` [width] wide gives to buttons of [weights], [gap] between them: each its part of
 * the width less the gaps, rounded, the pixels the rounding took too many or left out given back to or taken by the
 * first buttons, one each, as `Row` does.
 */
fun rowShares(
    width: Int,
    gap: Int,
    weights: List<Float>,
): List<Int> {
    val room = (width - gap * (weights.size - 1)).coerceAtLeast(0)
    val unit = room / weights.sum()
    var remainder = room - weights.sumOf { (unit * it).roundToInt() }
    return weights.map { weight ->
        val unitBack = remainder.sign
        remainder -= unitBack
        maxOf(0, (unit * weight).roundToInt() + unitBack)
    }
}

/**
 * Whether [labels], widths in pixels, hold in buttons of [weights] side by side on a row [width] wide, [gap] between
 * them, each label within its share ([rowShares]) less [padding] on each side.
 */
fun holdSideBySide(
    width: Int,
    gap: Int,
    padding: Int,
    weights: List<Float>,
    labels: List<Int>,
): Boolean = labelsHold(rowShares(width, gap, weights), labels, padding)

/** Whether each of [labels] holds in its share of [shares] less [padding] on each side, all in pixels. */
fun labelsHold(
    shares: List<Int>,
    labels: List<Int>,
    padding: Int,
): Boolean = shares.zip(labels).all { (share, label) -> label <= share - 2 * padding }

/** Between each side of a [FramedBox] and its label, given its [labelPadding], in pixels as the layout rounds it. */
fun Density.framedBoxPaddingPx(labelPadding: Dp): Int = framedBoxPadding(labelPadding).roundToPx()
