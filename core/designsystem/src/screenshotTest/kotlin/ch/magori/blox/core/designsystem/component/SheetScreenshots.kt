package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the sheets: the sheet Au hasard of the editor, come in, its choices and its action, by day and by
 * night in Couleurs, and in a font twice as large, where its choices scroll and its action stays in sight. Renaming a
 * test breaks its link to its reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun SheetColorful() = SheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT)
@Composable
private fun SheetColorfulNight() = SheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun SheetColorfulLargeFont() = SheetColorfulLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f, uiMode = UI_MODE_NIGHT)
@Composable
private fun SheetColorfulLargeFontNight() = SheetColorfulLargeFontPreview()
