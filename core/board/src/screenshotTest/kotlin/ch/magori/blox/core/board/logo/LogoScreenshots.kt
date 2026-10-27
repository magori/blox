package ch.magori.blox.core.board.logo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.board.mascot.MascotColorfulPreview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the logo BloX v2 and of the mascots (P5.77), by day and by night, from the previews of the
 * production code. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview
@Composable
private fun LogoColorful() = BloxLogoPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun LogoColorfulNight() = BloxLogoPreview()

@PreviewTest
@Preview
@Composable
private fun MascotsColorful() = MascotColorfulPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun MascotsColorfulNight() = MascotColorfulPreview()
