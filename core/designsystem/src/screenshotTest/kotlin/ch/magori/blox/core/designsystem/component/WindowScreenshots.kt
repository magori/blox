package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the windows shared by the editor and « Mes musiques », in Couleurs by day and by night, the font
 * of the system normal and twice as large: a name refused, its field framed in `danger`, with the free name offered,
 * and a deletion that failed, with its note, Supprimer filled in `danger` beside the trash. The answers stand one above
 * the other, the safe one first. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun NamingWindowTakenName() = NamingWindowPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT)
@Composable
private fun NamingWindowTakenNameNight() = NamingWindowPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun NamingWindowTakenNameLargeFont() = NamingWindowLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f, uiMode = UI_MODE_NIGHT)
@Composable
private fun NamingWindowTakenNameLargeFontNight() = NamingWindowLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun DeleteWindowFailed() = DeleteWindowPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT)
@Composable
private fun DeleteWindowFailedNight() = DeleteWindowPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun DeleteWindowFailedLargeFont() = DeleteWindowLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f, uiMode = UI_MODE_NIGHT)
@Composable
private fun DeleteWindowFailedLargeFontNight() = DeleteWindowLargeFontPreview()
