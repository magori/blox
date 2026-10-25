package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the windows shared by the editor and « Mes musiques », the font of the system normal and twice
 * as large: a name refused, with the free name offered (Original, and Couleurs twice as large), and a deletion that
 * failed, with its note (Couleurs, where `danger` is red, and Original twice as large, where the answers stand one
 * above the other). Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun NamingWindowTakenName() = NamingWindowPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun NamingWindowTakenNameLargeFont() = NamingWindowLargeFontPreview()

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
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun NamingWindowTakenNameColorfulLargeFont() = NamingWindowColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f, uiMode = UI_MODE_NIGHT)
@Composable
private fun NamingWindowTakenNameColorfulLargeFontNight() = NamingWindowColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun DeleteWindowFailedOriginalLargeFont() = DeleteWindowOriginalLargeFontPreview()
