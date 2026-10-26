package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the sliders by steps: the volume at 0, 50 and 100 % and disabled, the gauge of the level at its
 * lowest, in the middle and at its highest, by day and by night in Couleurs, in Original (where the bars beyond the
 * value are in button), and the font of the system twice as large. Renaming a test breaks its link to its reference
 * image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 560)
@Composable
private fun StepperOriginal() = StepperOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 560)
@Composable
private fun StepperColorful() = StepperColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 560, uiMode = UI_MODE_NIGHT)
@Composable
private fun StepperColorfulNight() = StepperColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun StepperColorfulLargeFont() = StepperColorfulPreview()
