package ch.magori.blox.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.feature.game.controls.GameControlsHeldPreview
import ch.magori.blox.feature.game.controls.GameControlsLeftHandedPreview
import ch.magori.blox.feature.game.gameover.GameOverColorfulNewRecordPreview
import ch.magori.blox.feature.game.gameover.GameOverColorfulPreview
import ch.magori.blox.feature.game.gameover.GameOverDailyNewBestPreview
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
import ch.magori.blox.feature.game.topbar.GameTopBarNewRecordLargeFontPreview
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
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenColorfulNight() = GameScreenColorfulPreview()

// A phone of 412 dp, its keys of a phone (60 and 68 dp); a phone of 390 dp, its keys halfway (56 and 62 dp).

@PreviewTest
@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun GameScreenColorfulWide() = GameScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 412, heightDp = 915, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenColorfulWideNight() = GameScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GameScreenColorfulHalfway() = GameScreenColorfulPreview()

// For a left handed player: B and A on the left, the arrows on the right.

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenLeftHanded() = GameScreenLeftHandedPreview()

@PreviewTest
@Preview(widthDp = 412, heightDp = 915, fontScale = 2f)
@Composable
private fun GameScreenLeftHandedLargeFont() = GameScreenLeftHandedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenPaused() = GameScreenPausedPreview()

// The pause (view V29): the curtain shut and the card, day and night; the curtain halfway down, in
// full effects (rows of empty cells) and in reduced ones (a fade), the card not shown yet.

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenPausedNight() = GameScreenPausedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenCurtain() = GameScreenCurtainPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenCurtainReduced() = GameScreenCurtainReducedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun GameScreenLargeFont() = GameScreenLargeFontPreview()

// The record beaten during the game (V28): NOUVEAU RECORD and the score in the top bar, the record bar full.

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenNewRecord() = GameScreenNewRecordPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenNewRecordNight() = GameScreenNewRecordPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenNewRecordOriginal() = GameScreenNewRecordOriginalPreview()

// The feedback of the game (V28), at fixed ticks: rows being cleared 4 ticks after the lock (cells lit, A turning, the
// middle cells gone, the next ones swelling), then cleared 4 ticks before (rows coming down, sticker « +600 », score
// counting); day, night and large font, and in reduced effects.

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenClearing() = GameScreenClearingPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenClearingNight() = GameScreenClearingPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun GameScreenClearingLargeFont() = GameScreenClearingPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenClearingReduced() = GameScreenClearingReducedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenCleared() = GameScreenClearedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenClearedNight() = GameScreenClearedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun GameScreenClearedLargeFont() = GameScreenClearedPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenClearedReduced() = GameScreenClearedReducedPreview()

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
@Preview(widthDp = 220, heightDp = 560, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenTooSmallColorfulNight() = GameScreenTooSmallColorfulPreview()

@PreviewTest
@Preview(widthDp = 220, heightDp = 560, fontScale = 2f)
@Composable
private fun GameScreenTooSmallLargeFont() = GameScreenTooSmallLargeFontPreview()

// The top bar with a large font.

@PreviewTest
@Preview(widthDp = 360, fontScale = 2f)
@Composable
private fun TopBarLargeFont() = GameTopBarLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, fontScale = 2f)
@Composable
private fun TopBarNewRecordLargeFont() = GameTopBarNewRecordLargeFontPreview()

// The side column: a score beyond 999 999, a large font, no next piece.

@PreviewTest
@Preview
@Composable
private fun SidePanelColorful() = SidePanelColorfulPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun SidePanelColorfulNight() = SidePanelColorfulPreview()

@PreviewTest
@Preview(fontScale = 2f)
@Composable
private fun SidePanelLargeFont() = SidePanelLargeFontPreview()

@PreviewTest
@Preview
@Composable
private fun SidePanelNoNextPiece() = SidePanelNoNextPiecePreview()

// The controls of a phone of 412 dp, with keys held, and for a left handed player.

@PreviewTest
@Preview(widthDp = 412)
@Composable
private fun ControlsHeld() = GameControlsHeldPreview()

@PreviewTest
@Preview(widthDp = 412, uiMode = UI_MODE_NIGHT)
@Composable
private fun ControlsHeldNight() = GameControlsHeldPreview()

@PreviewTest
@Preview(widthDp = 412)
@Composable
private fun ControlsLeftHanded() = GameControlsLeftHandedPreview()

// The card of the pause alone (view V29).

@PreviewTest
@Preview
@Composable
private fun PauseOriginal() = PauseOriginalPreview()

@PreviewTest
@Preview
@Composable
private fun PauseColorful() = PauseColorfulPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun PauseColorfulNight() = PauseColorfulPreview()

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
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun GameOverColorfulNight() = GameOverColorfulPreview()

@PreviewTest
@Preview
@Composable
private fun GameOverColorfulNewRecord() = GameOverColorfulNewRecordPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun GameOverColorfulNewRecordNight() = GameOverColorfulNewRecordPreview()

@PreviewTest
@Preview(fontScale = 2f)
@Composable
private fun GameOverLargeFont() = GameOverLargeFontPreview()

// A daily game (view V39), in the Colorful theme only, day and night: GRAINE DU JOUR and the best of the day in the
// top bar; its game over, MEILLEUR DU JOUR, then Rejouer, Copier and Menu.

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenDaily() = GameScreenDailyPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun GameScreenDailyNight() = GameScreenDailyPreview()

@PreviewTest
@Preview
@Composable
private fun GameOverDailyNewBest() = GameOverDailyNewBestPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun GameOverDailyNewBestNight() = GameOverDailyNewBestPreview()

/** The game over of a daily game, font enlarged: « Ancien meilleur 098760 » under the score still fits. */
@PreviewTest
@Preview(fontScale = 2f)
@Composable
private fun GameOverDailyLargeFont() = GameOverDailyNewBestPreview()
