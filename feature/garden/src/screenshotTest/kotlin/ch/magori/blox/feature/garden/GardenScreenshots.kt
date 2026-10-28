package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the musical garden (view V40), by day and by night, under the Original theme of the application:
 * the screen draws itself in the Colorful theme. The empty garden as it opens; four pieces and a preview refused over
 * one of them, a cell crossed out; the same in a font twice as large on a narrow phone; « Jardin effacé · Annuler »;
 * the garden listened to, its column played framed and the pill of its chord filled, by day and by night.
 * From the previews of the production code, with the same `@Preview` configuration. Renaming a test breaks its link to
 * its reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun GardenEmptyDay() = GardenEmptyPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
private fun GardenEmptyNight() = GardenEmptyNightPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenRefusedDay() = GardenRefusedPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun GardenRefusedNight() = GardenRefusedNightPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun GardenLargeFont() = GardenLargeFontPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenClearedDay() = GardenClearedPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenListeningDay() = GardenListeningPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun GardenListeningNight() = GardenListeningNightPreview()
