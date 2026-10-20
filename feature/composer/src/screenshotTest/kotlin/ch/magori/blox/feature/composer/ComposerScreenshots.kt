package ch.magori.blox.feature.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the music editor (views V8 and V11), and of its windows (save, V9 and V12, and the question before
 * leaving), in both themes coded so far, the dice rolled, its message, and the windows, also with the font twice as
 * large, from the previews of the production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
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
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerEmpty() = ComposerEmptyPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerDiceOriginal() = ComposerDiceOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerDiceColorful() = ComposerDiceColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun ComposerDiceOriginalLargeFont() = ComposerDiceOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun ComposerDiceColorfulLargeFont() = ComposerDiceColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun MessageOriginal() = MessageOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun MessageColorful() = MessageColorfulPreview()

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
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun SaveWindowOriginalLargeFont() = SaveWindowOriginalLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun SaveWindowColorful() = SaveWindowColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun SaveWindowColorfulLargeFont() = SaveWindowColorfulLargeFontPreview()

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
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun LeaveWindowColorfulLargeFont() = LeaveWindowColorfulLargeFontPreview()
