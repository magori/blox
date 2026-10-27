package ch.magori.blox.feature.cartridges

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the choice of the game (view V36), by day and by night, and with the font of the system twice as
 * large, under the Original theme of the application: the screen draws itself in the Colorful theme. The Graine du jour
 * bears its sticker NOUVELLE. From the previews of the production code, with the same `@Preview` configuration.
 * Renaming a test breaks its link to its reference image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun CartridgesDay() = CartridgesDayPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
private fun CartridgesNight() = CartridgesNightPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun CartridgesLargeFont() = CartridgesLargeFontPreview()
