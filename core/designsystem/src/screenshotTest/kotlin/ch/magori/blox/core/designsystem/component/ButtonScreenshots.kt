package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the bubble buttons: each family at rest, pressed, disabled and focused, those in accent
 * (Principal XL, Principal) and those in button (Secondaire, Icône), by day and by night in Couleurs, and square and
 * flat in Original. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 600)
@Composable
private fun AccentButtonsOriginal() = AccentButtonsOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 600)
@Composable
private fun AccentButtonsColorful() = AccentButtonsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 600, uiMode = UI_MODE_NIGHT)
@Composable
private fun AccentButtonsColorfulNight() = AccentButtonsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun PlainButtonsOriginal() = PlainButtonsOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun PlainButtonsColorful() = PlainButtonsColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360, uiMode = UI_MODE_NIGHT)
@Composable
private fun PlainButtonsColorfulNight() = PlainButtonsColorfulPreview()
