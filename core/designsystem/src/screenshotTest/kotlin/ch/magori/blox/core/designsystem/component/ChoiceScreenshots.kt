package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the groups of choices: a segment retained, checked in accent, one not yet available, with
 * BIENTÔT, a group beside its label and one of two rows under it, by day and by night in Couleurs, square in Original,
 * and in a font twice as large, where every label goes above its group; the segments without a label of the editor,
 * its tracks each in its colour and its tools with their icons. Renaming a test breaks its link to its
 * reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun ChoiceGroupsOriginal() = ChoiceGroupsOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun ChoiceGroupsColorful() = ChoiceGroupsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360, uiMode = UI_MODE_NIGHT)
@Composable
private fun ChoiceGroupsColorfulNight() = ChoiceGroupsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun ChoiceGroupsColorfulLargeFont() = ChoiceGroupsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun ChoiceGroupsOriginalLargeFont() = ChoiceGroupsOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun ChoiceSegmentsOriginal() = ChoiceSegmentsOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun ChoiceSegmentsColorful() = ChoiceSegmentsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 240, uiMode = UI_MODE_NIGHT)
@Composable
private fun ChoiceSegmentsColorfulNight() = ChoiceSegmentsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360, fontScale = 2f)
@Composable
private fun ChoiceSegmentsColorfulLargeFont() = ChoiceSegmentsColorfulPreview()
