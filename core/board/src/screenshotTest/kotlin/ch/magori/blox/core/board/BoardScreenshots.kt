package ch.magori.blox.core.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the board, from the previews of the production code. The whole game screen
 * (feature:game) shows the board in both themes: these captures are the board alone, larger than on the
 * screen. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

// The board alone, larger than on the screen: the cells of the Original theme in detail.

@PreviewTest
@Preview
@Composable
private fun BoardOriginal() = BoardCanvasOriginalPreview()

// The bubbles of the Colorful theme, by day and by night: cells of 24 dp, with their gloss.

@PreviewTest
@Preview
@Composable
private fun BoardColorful() = BoardCanvasColorfulPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun BoardColorfulNight() = BoardCanvasColorfulPreview()

// Cells under 16 dp: corners of 2 dp, without gloss.

@PreviewTest
@Preview
@Composable
private fun BoardColorfulSmall() = BoardCanvasColorfulSmallPreview()
