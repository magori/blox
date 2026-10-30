package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the screen of the Locksmith (view V35), by day and by night, under the Original theme of the
 * application: the screen draws itself in the Colorful theme. A preview refused, its cells on the ring drawn over the
 * rim, on the side and at the bottom, clear of the message; a hint on a placed piece that blocks; the padlock open, its
 * card, and on a small screen in large text, where the card keeps to its share of the height. A preview refused on the
 * ring at the left and at the right, cut at the edge of the screen, and shaken to each side, narrowed rather than out of
 * its margin. The feedback of an action, frozen at one tick: a placement, a refusal, an opening, a hint, and in reduced
 * effects a refusal and an opening. A padlock of 6 pieces on a phone of 360 dp, its tiles shrunk to hold on one row, and
 * on 320 dp, where they go on two rows; a padlock of 5 pieces on 300 dp, its last row centred. The card of an open
 * padlock with what the progress tells (P7.33): PARFAITE, NOUVEAU MEILLEUR, an old padlock replayed (its best, Reprendre
 * à la serrure N), by day, by night and on a small screen in large text, and an opening not recorded. From the previews
 * of the production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference
 * image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedDay() = LocksmithRefusedDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun LocksmithRefusedNight() = LocksmithRefusedNightPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithBlockingDay() = LocksmithBlockingDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun LocksmithBlockingNight() = LocksmithBlockingNightPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithOpenDay() = LocksmithOpenDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun LocksmithOpenNight() = LocksmithOpenNightPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedBottomDay() = LocksmithRefusedBottomDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun LocksmithRefusedBottomNight() = LocksmithRefusedBottomNightPreview()

@PreviewTest
@Preview(widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
private fun LocksmithOpenSmallDay() = LocksmithOpenSmallDayPreview()

@PreviewTest
@Preview(widthDp = 320, heightDp = 480, fontScale = 2f, uiMode = UI_MODE_NIGHT)
@Composable
private fun LocksmithOpenSmallNight() = LocksmithOpenSmallNightPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedLeftDay() = LocksmithRefusedLeftDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedRightDay() = LocksmithRefusedRightDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithPlacedFeedbackDay() = LocksmithPlacedFeedbackDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedShakeDay() = LocksmithRefusedShakeDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedReducedDay() = LocksmithRefusedReducedDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithOpenWaveDay() = LocksmithOpenWaveDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithOpenReducedDay() = LocksmithOpenReducedDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithHintSwellDay() = LocksmithHintSwellDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedLeftShakeDay() = LocksmithRefusedLeftShakeDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedLeftReducedDay() = LocksmithRefusedLeftReducedDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithRefusedRightShakeDay() = LocksmithRefusedRightShakeDayPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LocksmithSixTilesDay() = LocksmithSixTilesDayPreview()

@PreviewTest
@Preview(widthDp = 320, heightDp = 800)
@Composable
private fun LocksmithSixTilesNarrowDay() = LocksmithSixTilesNarrowDayPreview()

@PreviewTest
@Preview(widthDp = 300, heightDp = 800)
@Composable
private fun LocksmithFiveTilesNarrowDay() = LocksmithFiveTilesNarrowDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithOpenPerfectDay() = LocksmithOpenPerfectDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithOpenNewBestDay() = LocksmithOpenNewBestDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithOpenReplayDay() = LocksmithOpenReplayDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun LocksmithOpenReplayNight() = LocksmithOpenReplayNightPreview()

@PreviewTest
@Preview(widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
private fun LocksmithOpenReplaySmallDay() = LocksmithOpenReplaySmallDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithOpenUnrecordedDay() = LocksmithOpenUnrecordedDayPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun LocksmithBarLargeFontDay() = LocksmithBarLargeFontDayPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LocksmithChordsWrapDay() = LocksmithChordsWrapDayPreview()
