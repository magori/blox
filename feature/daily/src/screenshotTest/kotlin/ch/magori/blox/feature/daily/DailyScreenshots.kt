package ch.magori.blox.feature.daily

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the entry of the Graine du jour (view V38), by day and by night, under the Original theme of the
 * application: the screen draws itself in the Colorful theme. Before any try (« — », copying disabled with its line),
 * after five tries, and in a font twice as large. From the previews of the production code, with the same `@Preview`
 * configuration. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun DailyBeforeAnyTryDay() = DailyBeforeAnyTryPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
private fun DailyBeforeAnyTryNight() = DailyBeforeAnyTryNightPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun DailyAfterTriesDay() = DailyAfterTriesPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
private fun DailyAfterTriesNight() = DailyAfterTriesNightPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
private fun DailyLargeFontDay() = DailyLargeFontPreview()
