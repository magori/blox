package ch.magori.blox.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the settings page (view V21), in both themes and in a font twice as large, from the previews of
 * the production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun SettingsOriginal() = SettingsScreenOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun SettingsColorful() = SettingsScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun SettingsLargeFont() = SettingsScreenLargeFontPreview()
