package ch.magori.blox.core.designsystem.icon

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/** The night of the system, where the Colorful theme takes its dark variant (ADR 0020). */
private const val NIGHT = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL

/*
 * Screenshot tests of the icons, every one in each theme, at the sizes the screens give them and large: in full pixels,
 * edges sharp, in Original; in rounded strokes in Couleurs. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 1100)
@Composable
private fun IconSheetOriginal() = IconSheetOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1100)
@Composable
private fun IconSheetColorful() = IconSheetColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1100, uiMode = NIGHT)
@Composable
private fun IconSheetColorfulNight() = IconSheetColorfulPreview()
