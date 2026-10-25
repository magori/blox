package ch.magori.blox.feature.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/** The night of the system, where the Colorful theme takes its dark variant (ADR 0020). */
private const val NIGHT = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL

/*
 * Screenshot tests of the home screen (view V1), in both themes, from the previews of the
 * production code, with the same `@Preview` configuration. Renaming a test breaks its link to its
 * reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun HomeOriginal() = HomeScreenOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun HomeColorful() = HomeScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = NIGHT)
@Composable
private fun HomeColorfulNight() = HomeScreenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1300, fontScale = 2f)
@Composable
private fun HomeLargeFont() = HomeScreenLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 880)
@Composable
private fun HomeGameMusic() = HomeScreenGameMusicPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 880)
@Composable
private fun HomeGameMusicLongName() = HomeScreenGameMusicLongNamePreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1380, fontScale = 2f)
@Composable
private fun HomeGameMusicLongNameLargeFont() = HomeScreenGameMusicLongNameLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1300, fontScale = 2f)
@Composable
private fun HomeColorfulLargeFont() = HomeScreenColorfulLargeFontPreview()
