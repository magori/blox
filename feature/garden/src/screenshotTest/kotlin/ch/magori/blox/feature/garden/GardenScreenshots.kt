package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the musical garden (view V40), by day and by night, under the Original theme of the application:
 * the screen draws itself in the Colorful theme. The empty garden as it opens; four pieces and a preview refused over
 * one of them, a cell crossed out; the same in a font twice as large on a narrow phone; « Jardin effacé · Annuler »;
 * the garden listened to, its column played framed and the pill of its chord filled, by day and by night. The
 * feedbacks frozen on one frame: a piece placed lit while the notes of the column played bounce, by day and by night;
 * the pieces of Au hasard falling one by one, or fading in together in reduced effects.
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

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenPlacedDay() = GardenPlacedPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun GardenPlacedNight() = GardenPlacedNightPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenRolledDay() = GardenRolledPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenRolledReduced() = GardenRolledReducedPreview()

/*
 * « Mes jardins » (P10.29): the dot of its disc on a narrow phone, in a font once and twice as large, where the bar
 * holds 5 parts; « « X » gardé. »; the sheet, its list by day and by night and in a large font, empty, full, of a later
 * version; the window of the name.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun GardenModified() = GardenModifiedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun GardenModifiedLargeFont() = GardenModifiedLargeFontPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenKept() = GardenKeptPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MyGardensList() = MyGardensListPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun MyGardensListNight() = MyGardensListNightPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun MyGardensListLargeFont() = MyGardensListLargeFontPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MyGardensEmpty() = MyGardensEmptyPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MyGardensFull() = MyGardensFullPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MyGardensUnreadable() = MyGardensUnreadablePreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GardenNaming() = GardenNamingPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun GardenNamingLargeFont() = GardenNamingLargeFontPreview()
