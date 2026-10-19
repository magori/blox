package ch.magori.blox.feature.game.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTypographies

/**
 * The smallest size to which a text of [maxFontSize] shrinks to fit its place, at this density and font scale:
 * [BloxTypographies.MinimumFontSize], to stay readable, unless the system font is so enlarged that this floor
 * is larger than the text with the normal font. The text may then shrink back to its size with the normal
 * font, never smaller: in the 92 dp of a panel, the 8 letters of « SUIVANTE » fit at 11 dp, not at 8 sp × 2.
 * Never above [maxFontSize]: a text smaller than the floor keeps its size.
 */
internal fun Density.shrinkFloor(maxFontSize: TextUnit): TextUnit {
    val normalSize = maxFontSize.value.dp.toSp()
    val floor = if (normalSize < BloxTypographies.MinimumFontSize) normalSize else BloxTypographies.MinimumFontSize
    return if (floor > maxFontSize) maxFontSize else floor
}

/** A text on its lines from [maxFontSize], shrinking to fit its place down to [shrinkFloor]. */
@Composable
@ReadOnlyComposable
internal fun shrinkToFit(maxFontSize: TextUnit): TextAutoSize = TextAutoSize.StepBased(
    minFontSize = LocalDensity.current.shrinkFloor(maxFontSize),
    maxFontSize = maxFontSize,
)
