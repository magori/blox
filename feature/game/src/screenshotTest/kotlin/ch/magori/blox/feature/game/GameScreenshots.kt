package ch.magori.blox.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.feature.game.controls.GameControlsHeldPreview
import ch.magori.blox.feature.game.gameover.GameOverColorfulNewRecordPreview
import ch.magori.blox.feature.game.gameover.GameOverColorfulPreview
import ch.magori.blox.feature.game.gameover.GameOverLargeFontPreview
import ch.magori.blox.feature.game.gameover.GameOverOriginalNewRecordPreview
import ch.magori.blox.feature.game.gameover.GameOverOriginalPreview
import ch.magori.blox.feature.game.pause.PauseColorfulPreview
import ch.magori.blox.feature.game.pause.PauseLargeFontPreview
import ch.magori.blox.feature.game.pause.PauseOriginalPreview
import ch.magori.blox.feature.game.sidepanel.SidePanelColorfulPreview
import ch.magori.blox.feature.game.sidepanel.SidePanelLargeFontPreview
import ch.magori.blox.feature.game.sidepanel.SidePanelNoNextPiecePreview
import ch.magori.blox.feature.game.topbar.GameTopBarLargeFontPreview
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the game screen and its parts, in both themes, from the previews of the
 * production code: each test renders one preview with the same `@Preview` configuration (size,
 * font scale), so that the captures show what Android Studio shows. Renaming a test breaks its link
 * to its reference image: regenerate them with `./gradlew updateDebugScreenshotTest`, and delete the
 * images of the tests removed or renamed.
 *
 * The whole screen shows the top bar, the board, the side column and the controls in both themes:
 * the parts have captures of their own only for what it does not show.
 */

// The whole screen (views V2, V3).

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenOriginal() = GameScreenOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenColorful() = GameScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenPaused() = GameScreenPausedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun GameScreenLargeFont() = GameScreenLargeFontPreview()

// Landscape on a phone of 360 dp, with a large font: the next piece apart, on the right of the board.

@PreviewTest
@Preview(widthDp = 800, heightDp = 360, fontScale = 2f)
@Composable
private fun GameScreenLandscapeLargeFont() = GameScreenLandscapeLargeFontPreview()

// The narrowest multi-window, 220 dp: the score above the board, two by two, in both themes, and with a
// large font (P2.20).

@PreviewTest
@Preview(widthDp = 220, heightDp = 640)
@Composable
private fun GameScreenNarrow() = GameScreenNarrowPreview()

@PreviewTest
@Preview(widthDp = 220, heightDp = 640)
@Composable
private fun GameScreenNarrowOriginal() = GameScreenNarrowOriginalPreview()

@PreviewTest
@Preview(widthDp = 220, heightDp = 640, fontScale = 2f)
@Composable
private fun GameScreenNarrowLargeFont() = GameScreenNarrowLargeFontPreview()

// The narrowest multi-window, 220 dp, too low to play: « Agrandissez la fenêtre » in place of the board, in both
// themes, and with a large font (P2.24).

@PreviewTest
@Preview(widthDp = 220, heightDp = 560)
@Composable
private fun GameScreenTooSmallOriginal() = GameScreenTooSmallOriginalPreview()

@PreviewTest
@Preview(widthDp = 220, heightDp = 560)
@Composable
private fun GameScreenTooSmallColorful() = GameScreenTooSmallColorfulPreview()

@PreviewTest
@Preview(widthDp = 220, heightDp = 560, fontScale = 2f)
@Composable
private fun GameScreenTooSmallLargeFont() = GameScreenTooSmallLargeFontPreview()

// The top bar with a large font.

@PreviewTest
@Preview(widthDp = 360, fontScale = 2f)
@Composable
private fun TopBarLargeFont() = GameTopBarLargeFontPreview()

// The side column: a score beyond 999 999, a large font, no next piece.

@PreviewTest
@Preview
@Composable
private fun SidePanelColorful() = SidePanelColorfulPreview()

@PreviewTest
@Preview(fontScale = 2f)
@Composable
private fun SidePanelLargeFont() = SidePanelLargeFontPreview()

@PreviewTest
@Preview
@Composable
private fun SidePanelNoNextPiece() = SidePanelNoNextPiecePreview()

// The controls, with keys held.

@PreviewTest
@Preview(widthDp = 360)
@Composable
private fun ControlsHeld() = GameControlsHeldPreview()

// The pause screen.

@PreviewTest
@Preview
@Composable
private fun PauseOriginal() = PauseOriginalPreview()

@PreviewTest
@Preview
@Composable
private fun PauseColorful() = PauseColorfulPreview()

@PreviewTest
@Preview(fontScale = 2f)
@Composable
private fun PauseLargeFont() = PauseLargeFontPreview()

// The game over screen (view V4).

@PreviewTest
@Preview
@Composable
private fun GameOverOriginal() = GameOverOriginalPreview()

@PreviewTest
@Preview
@Composable
private fun GameOverOriginalNewRecord() = GameOverOriginalNewRecordPreview()

@PreviewTest
@Preview
@Composable
private fun GameOverColorful() = GameOverColorfulPreview()

@PreviewTest
@Preview
@Composable
private fun GameOverColorfulNewRecord() = GameOverColorfulNewRecordPreview()

@PreviewTest
@Preview(fontScale = 2f)
@Composable
private fun GameOverLargeFont() = GameOverLargeFontPreview()
