package ch.magori.blox.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the icons, every one in each theme, at the sizes the screens give them and large: in full pixels,
 * edges sharp, in Original; in rounded strokes in Couleurs. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 1600)
@Composable
private fun IconSheetOriginal() = IconSheetOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1600)
@Composable
private fun IconSheetColorful() = IconSheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1600, uiMode = UI_MODE_NIGHT)
@Composable
private fun IconSheetColorfulNight() = IconSheetColorfulPreview()
