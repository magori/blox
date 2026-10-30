package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the settings rows and their switches: on and off, at rest and pressed, disabled with their
 * reason, by day and by night, and in a font twice as large; in Couleurs alone, the theme of the settings page (P12.13).
 * Renaming a test breaks its link to its reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun SwitchesColorful() = SwitchesColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 720, uiMode = UI_MODE_NIGHT)
@Composable
private fun SwitchesColorfulNight() = SwitchesColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1600, fontScale = 2f)
@Composable
private fun SwitchesColorfulLargeFont() = SwitchesColorfulLargeFontPreview()
