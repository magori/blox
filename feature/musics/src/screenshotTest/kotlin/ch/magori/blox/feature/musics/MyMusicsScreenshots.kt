package ch.magori.blox.feature.musics

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of « Mes musiques » (views V7 and V10), in both themes coded so far, with the font of the system
 * normal and twice as large, from the previews of the production code, with the same `@Preview` configuration.
 * Renaming a test breaks its link to its reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun MyMusicsOriginal() = MyMusicsOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun MyMusicsColorful() = MyMusicsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun MyMusicsColorfulNight() = MyMusicsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun MyMusicsOriginalLargeFont() = MyMusicsOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun MyMusicsColorfulLargeFont() = MyMusicsColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun MyMusicsEmpty() = MyMusicsEmptyPreview()
