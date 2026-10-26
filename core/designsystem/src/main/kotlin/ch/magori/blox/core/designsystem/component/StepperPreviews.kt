package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** The levels of the sheet: from 0 to 9, as the Classic offers them. */
private val SheetLevels = 0..9

/**
 * The sliders of the theme [kind]: a volume at 0, 50 and 100 % and one disabled, then the gauge of the level at its
 * lowest, in the middle and at its highest.
 */
@Composable
private fun StepperSheet(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        val palette = BloxTheme.palette
        Column(
            modifier = Modifier.fillMaxSize().background(palette.page).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Volume", color = palette.label, style = BloxTheme.typography.section)
            listOf(0, 50, 100).forEach { percent ->
                VolumeSlider(percent = percent, onPercentChange = {}, description = "Volume")
            }
            VolumeSlider(percent = 30, onPercentChange = {}, description = "Volume", enabled = false)
            Text(text = "Niveau", color = palette.label, style = BloxTheme.typography.section)
            listOf(SheetLevels.first, 5, SheetLevels.last).forEach { level ->
                LevelGauge(level = level, levels = SheetLevels, onLevelChange = {}, description = "Niveau")
            }
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 560)
@Composable
internal fun StepperOriginalPreview() {
    StepperSheet(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 560)
@Composable
internal fun StepperColorfulPreview() {
    StepperSheet(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 560)
@Composable
internal fun StepperColorfulRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        StepperSheet(BloxThemeKind.COLORFUL)
    }
}
