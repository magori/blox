package ch.magori.blox.core.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the board, from the previews of the production code. The whole game screen
 * (feature:game) shows the board in both themes: this capture is the board alone, larger than on the
 * screen. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

// The board alone, larger than on the screen: the cells of the Original theme in detail.

@PreviewTest
@Preview
@Composable
private fun BoardOriginal() = BoardCanvasOriginalPreview()
