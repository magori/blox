package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the menu « … » and of the messages that go away (P5.97), in Couleurs, by day, by night, and the
 * font of the system twice as large: the items of a card and of the editor, checked or not, Supprimer in danger; a
 * message with Annuler and its cross, whose action goes under its text in a large font, and a message alone. Renaming
 * a test breaks its link to its reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 560)
@Composable
private fun MenuColorful() = MenuPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 560, uiMode = UI_MODE_NIGHT)
@Composable
private fun MenuColorfulNight() = MenuPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun MenuColorfulLargeFont() = MenuPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun SnackMessageColorful() = SnackMessagePreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 360, uiMode = UI_MODE_NIGHT)
@Composable
private fun SnackMessageColorfulNight() = SnackMessagePreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun SnackMessageColorfulLargeFont() = SnackMessagePreview()
