package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import kotlin.math.floor

/**
 * A text from [maxFontSize], shrinking to fit its place down to [shrinkFloor]. Prefers one line; wraps (up to the
 * maxLines of the text), between its words, only if one line would need a size below the floor.
 */
@Composable
@ReadOnlyComposable
fun shrinkToFewestLines(maxFontSize: TextUnit): TextAutoSize = shrinkingAutoSize(maxFontSize, ::FewestLines)

/** Lays the text out for [fewestLinesSize], each size once per measure: the second search tries some sizes again. */
private data class FewestLines(val minFontSize: TextUnit, val maxFontSize: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        val layouts = HashMap<Float, TextLayoutResult>()
        val layout = { size: Float -> layouts.getOrPut(size) { performLayout(constraints, text, size.toSp()) } }
        val size = fewestLinesSize(
            smallest = minFontSize.toPx(),
            largest = maxFontSize.toPx(),
            step = ShrinkStep.toPx(),
            fitsOnOneLine = { layout(it).run { fitsWithWholeWords && lineCount == 1 } },
            fitsOnLines = { layout(it).fitsWithWholeWords },
        )
        return size.toSp()
    }
}

/**
 * The size of a text, from [smallest] up by [step] to [largest] at most: the largest at which it [fitsOnOneLine], or
 * else the largest at which it [fitsOnLines], or else [smallest]. The lines are tried only if one line fits at no size.
 */
internal fun fewestLinesSize(
    smallest: Float,
    largest: Float,
    step: Float,
    fitsOnOneLine: (Float) -> Boolean,
    fitsOnLines: (Float) -> Boolean,
): Float = largestSizeThatFits(smallest, largest, step, fitsOnOneLine)
    ?: largestSizeThatFits(smallest, largest, step, fitsOnLines)
    ?: smallest

/**
 * The largest size, from [smallest] up by [step] to [largest] at most, at which a text [fits], or null if it fits at
 * none: it fits at every size below one at which it fits.
 */
internal fun largestSizeThatFits(
    smallest: Float,
    largest: Float,
    step: Float,
    fits: (Float) -> Boolean,
): Float? {
    if (fits(largest)) return largest
    if (!fits(smallest)) return null
    // The largest step at which it fits lies between low, where it does, and high.
    var low = 0
    var high = floor((largest - smallest) / step).toInt()
    while (low < high) {
        val middle = (low + high + 1) / 2
        if (fits(smallest + middle * step)) low = middle else high = middle - 1
    }
    return smallest + low * step
}

/**
 * A text from [maxFontSize], on as many lines as it needs (up to its maxLines), shrinking only when it does not fit
 * there or would cut a word, down to [shrinkFloor]. Unlike [shrinkToFewestLines], it does not seek the fewest lines:
 * it keeps the largest size, on as many lines as that size takes.
 */
@Composable
@ReadOnlyComposable
fun shrinkToWholeWords(maxFontSize: TextUnit): TextAutoSize = shrinkingAutoSize(maxFontSize, ::WholeWords)

/** Lays the text out for the largest size at which it fits with whole words, or else its smallest size. */
private data class WholeWords(val minFontSize: TextUnit, val maxFontSize: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit = largestSizeThatFits(
        smallest = minFontSize.toPx(),
        largest = maxFontSize.toPx(),
        step = ShrinkStep.toPx(),
        fits = { performLayout(constraints, text, it.toSp()).fitsWithWholeWords },
    )?.toSp() ?: minFontSize
}

/** Whether this text, as laid out, fits its place without cutting a word. */
private val TextLayoutResult.fitsWithWholeWords: Boolean
    get() = !didOverflowWidth && !didOverflowHeight && !cutsAWord
