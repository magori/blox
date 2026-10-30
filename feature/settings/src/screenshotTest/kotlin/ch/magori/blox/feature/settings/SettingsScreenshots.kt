package ch.magori.blox.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the settings page (views V21, V31), in both themes, by day and by night, in a font twice as
 * large, and with its greyed settings (Effets réduits imposed by Android among them), from the previews of the
 * production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun SettingsOriginal() = SettingsScreenOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun SettingsColorful() = SettingsScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200, uiMode = UI_MODE_NIGHT)
@Composable
private fun SettingsColorfulNight() = SettingsScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 2400, fontScale = 2f)
@Composable
private fun SettingsLargeFont() = SettingsScreenLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 2400, fontScale = 2f)
@Composable
private fun SettingsColorfulLargeFont() = SettingsScreenColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun SettingsGreyed() = SettingsScreenGreyedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200, uiMode = UI_MODE_NIGHT)
@Composable
private fun SettingsGreyedNight() = SettingsScreenGreyedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun SettingsOriginalGreyed() = SettingsScreenOriginalGreyedPreview()

// Gaucher on: its sub-line and its sketch turned over, by day and by night.

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun SettingsLeftHanded() = SettingsScreenLeftHandedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200, uiMode = UI_MODE_NIGHT)
@Composable
private fun SettingsLeftHandedNight() = SettingsScreenLeftHandedPreview()

// Opened without the section of a game (P12.3): Tous les jeux alone, no record.

@PreviewTest
@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun SettingsAllGames() = SettingsScreenAllGamesPreview()
