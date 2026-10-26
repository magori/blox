package ch.magori.blox.feature.cartridges

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the choice of the game (view V26), in Original and in Colorful by day and by night, with the font
 * of the system normal and twice as large, from the previews of the production code, with the same `@Preview`
 * configuration. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun CartridgesOriginal() = CartridgesOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun CartridgesColorful() = CartridgesColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun CartridgesColorfulNight() = CartridgesColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun CartridgesOriginalLargeFont() = CartridgesOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun CartridgesColorfulLargeFont() = CartridgesColorfulLargeFontPreview()
