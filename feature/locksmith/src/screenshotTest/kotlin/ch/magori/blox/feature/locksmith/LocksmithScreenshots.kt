package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the screen of the Locksmith (view V35), by day and by night, under the Original theme of the
 * application: the screen draws itself in the Colorful theme. A preview refused, its cells on the ring drawn over the
 * rim, on the side and at the bottom, clear of the message; a hint on a placed piece that blocks; the padlock open, its
 * card, and on a small screen in large text, where the card keeps to its share of the height. From the previews of the
 * production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
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
