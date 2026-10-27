package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of Écouter and of the equalizer (P5.94): the disc at rest, playing and disabled, in 48 and 56 dp,
 * by day and by night in Couleurs, and square and flat in Original. Renaming a test breaks its link to its reference
 * image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 200)
@Composable
private fun ListenOriginal() = ListenOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 200)
@Composable
private fun ListenColorful() = ListenColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 200, uiMode = UI_MODE_NIGHT)
@Composable
private fun ListenColorfulNight() = ListenColorfulPreview()
