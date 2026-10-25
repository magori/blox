package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the reliefs (button-gloss, button-gloss-pressed, cell-gloss), drawn as inner shadows: by day and
 * by night in Couleurs, and none in Original, flat and square. Renaming a test breaks its link to its reference
 * image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 420)
@Composable
private fun GlossSheetOriginal() = GlossSheetOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 420)
@Composable
private fun GlossSheetColorful() = GlossSheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 420, uiMode = UI_MODE_NIGHT)
@Composable
private fun GlossSheetColorfulNight() = GlossSheetColorfulPreview()
