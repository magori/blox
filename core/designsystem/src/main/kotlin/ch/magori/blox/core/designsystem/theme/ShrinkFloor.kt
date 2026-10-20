package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The smallest size to which a text of [maxFontSize] shrinks to fit its place, at this density and font scale:
 * [BloxTypographies.MinimumFontSize], to stay readable, unless the system font is so enlarged that this floor
 * is larger than the text with the normal font. The text may then shrink back to its size with the normal
 * font, never smaller: in the 92 dp of a panel, the 8 letters of « SUIVANTE » fit at 11 dp, not at 8 sp × 2.
 * Never above [maxFontSize]: a text smaller than the floor keeps its size.
 */
fun Density.shrinkFloor(maxFontSize: TextUnit): TextUnit {
    val normalSize = notEnlarged(maxFontSize)
    return if (normalSize < BloxTypographies.MinimumFontSize) normalSize else BloxTypographies.MinimumFontSize
}

/**
 * [size], but never larger on screen than with the normal font, at this density and font scale: where every dp of
 * height counts, as in the narrowest window, a text may keep the size it has with the normal font. Never above
 * [size]: with a font made smaller, the text keeps its size.
 */
fun Density.notEnlarged(size: TextUnit): TextUnit {
    val normalSize = size.value.dp.toSp()
    return if (normalSize < size) normalSize else size
}

/** A text on its lines from [maxFontSize], shrinking to fit its place down to [shrinkFloor]. */
@Composable
@ReadOnlyComposable
fun shrinkToFit(maxFontSize: TextUnit): TextAutoSize = shrinkingAutoSize(maxFontSize) { minFontSize, max ->
    TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = max, stepSize = ShrinkStep)
}

/**
 * A text on its lines from [maxFontSize] but no larger than with the normal font ([notEnlarged]), shrinking to fit its
 * place down to [shrinkFloor], as [shrinkToFit].
 */
@Composable
@ReadOnlyComposable
fun shrinkToFitNotEnlarged(maxFontSize: TextUnit): TextAutoSize {
    val notEnlarged = LocalDensity.current.notEnlarged(maxFontSize)
    return shrinkingAutoSize(maxFontSize) { minFontSize, _ ->
        TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = notEnlarged, stepSize = ShrinkStep)
    }
}

/** The step between two sizes tried by a text that shrinks, the default of `TextAutoSize.StepBased`. */
internal val ShrinkStep = 0.25.sp

/** The [TextAutoSize] made by [autoSize] for a text from [maxFontSize] down to [shrinkFloor], at this density. */
@Composable
@ReadOnlyComposable
internal fun shrinkingAutoSize(
    maxFontSize: TextUnit,
    autoSize: (minFontSize: TextUnit, maxFontSize: TextUnit) -> TextAutoSize,
): TextAutoSize = autoSize(LocalDensity.current.shrinkFloor(maxFontSize), maxFontSize)
