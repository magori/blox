package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the screen « Serrures » (P7.35), under the Original theme of the application: the screen draws
 * itself in the Colorful theme. Twelve padlocks open, three perfectly, the player coming from padlock 5, by day and by
 * night; no padlock open; a small screen in large text, its grid on two columns; a screen as low as a phone in
 * landscape in large text, a row of tiles shown whole; the longest words of a tile in large text; the window that asks
 * whether to clear the progress (P7.37), by day, by night, in large text, and once its clear failed. From the previews
 * of the production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference
 * image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PadlocksDay() = PadlocksDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun PadlocksNight() = PadlocksNightPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PadlocksEmptyDay() = PadlocksEmptyDayPreview()

@PreviewTest
@Preview(widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
private fun PadlocksSmallDay() = PadlocksSmallDayPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 400, fontScale = 2f)
@Composable
private fun PadlocksLowDay() = PadlocksLowDayPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun PadlocksLargeFontDay() = PadlocksLargeFontDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PadlocksClearDay() = PadlocksClearDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun PadlocksClearNight() = PadlocksClearNightPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun PadlocksClearLargeFontDay() = PadlocksClearLargeFontDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PadlocksClearFailedDay() = PadlocksClearFailedDayPreview()
