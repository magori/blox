package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the musical garden (view V40), by day and by night, under the Original theme of the application:
 * the screen draws itself in the Colorful theme. Its skeleton for now: the bar alone (P10.9). From the previews of the
 * production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun GardenEmptyDay() = GardenEmptyPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
private fun GardenEmptyNight() = GardenEmptyNightPreview()
