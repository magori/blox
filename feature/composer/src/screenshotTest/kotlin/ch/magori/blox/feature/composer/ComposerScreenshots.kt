package ch.magori.blox.feature.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the music editor (views V8, V11 and V33), and of its windows (save, V9 and V12, and the question
 * before leaving), in both themes coded so far, the sheet of the dice (V33, Couleurs only), the message of the dice, and
 * the windows, also with the font twice as large, from the previews of the production code, with the same `@Preview`
 * configuration. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerOriginal() = ComposerOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerColorful() = ComposerColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun ComposerColorfulNight() = ComposerColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerEmpty() = ComposerEmptyPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun DiceSheetColorful() = DiceSheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun DiceSheetColorfulNight() = DiceSheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun DiceSheetColorfulLargeFont() = DiceSheetColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun MessageOriginal() = MessageOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun MessageColorful() = MessageColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240, uiMode = UI_MODE_NIGHT)
@Composable
private fun MessageColorfulNight() = MessageColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 400, fontScale = 2f)
@Composable
private fun MessageOriginalLargeFont() = MessageOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 400, fontScale = 2f)
@Composable
private fun MessageColorfulLargeFont() = MessageColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun SaveWindowOriginal() = SaveWindowOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
private fun SaveWindowOriginalLargeFont() = SaveWindowOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun SaveWindowColorful() = SaveWindowColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT)
@Composable
private fun SaveWindowColorfulNight() = SaveWindowColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
private fun SaveWindowColorfulLargeFont() = SaveWindowColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
private fun SaveWindowLongestNameOriginalLargeFont() = SaveWindowLongestNameOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
private fun SaveWindowLongestNameColorfulLargeFont() = SaveWindowLongestNameColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun LeaveWindowOriginal() = LeaveWindowOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun LeaveWindowOriginalLargeFont() = LeaveWindowOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun LeaveWindowColorful() = LeaveWindowColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT)
@Composable
private fun LeaveWindowColorfulNight() = LeaveWindowColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun LeaveWindowColorfulLargeFont() = LeaveWindowColorfulLargeFontPreview()
