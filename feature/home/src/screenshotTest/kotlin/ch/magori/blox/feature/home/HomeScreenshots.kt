package ch.magori.blox.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the home screen (view V27), in both themes, from the previews of the
 * production code, with the same `@Preview` configuration. Renaming a test breaks its link to its
 * reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun HomeOriginal() = HomeScreenOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun HomeColorful() = HomeScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, uiMode = UI_MODE_NIGHT)
@Composable
private fun HomeColorfulNight() = HomeScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1700, fontScale = 2f)
@Composable
private fun HomeLargeFont() = HomeScreenLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1080)
@Composable
private fun HomeGameMusic() = HomeScreenGameMusicPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1080)
@Composable
private fun HomeGameMusicLongName() = HomeScreenGameMusicLongNamePreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1780, fontScale = 2f)
@Composable
private fun HomeGameMusicLongNameLargeFont() = HomeScreenGameMusicLongNameLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1700, fontScale = 2f)
@Composable
private fun HomeColorfulLargeFont() = HomeScreenColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun HomeMusicSheetColorful() = HomeMusicSheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, uiMode = UI_MODE_NIGHT)
@Composable
private fun HomeMusicSheetColorfulNight() = HomeMusicSheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun HomeMusicSheetOriginal() = HomeMusicSheetOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun HomeMusicSheetColorfulLargeFont() = HomeMusicSheetColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun HomeMusicSheetLongFileNameLargeFont() = HomeMusicSheetLongFileNameLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun HomeMusicSheetGameMusic() = HomeMusicSheetGameMusicPreview()
