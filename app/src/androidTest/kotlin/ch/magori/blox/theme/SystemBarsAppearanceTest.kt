package ch.magori.blox.theme

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * The real appearance of the system bars, read from the window, for the variant drawn. The mode of the system is
 * passed to [BloxTheme] (`darkSystem`): the test does not depend on the mode of the device, nor changes it.
 */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: the dex refuses spaces in method names below API 30 (minSdk 26).
internal class SystemBarsAppearanceTest {
    @ParameterizedTest(name = "{0}, system in dark mode: {1} -> light appearance (dark icons): {2}")
    @CsvSource(
        "ORIGINAL, true, true",
        "COLORFUL, false, true",
        "COLORFUL, true, false",
    )
    @DisplayName("the system bars take the appearance of the variant drawn, not that of the mode of the system")
    fun theSystemBarsFollowTheVariant(
        kind: BloxThemeKind,
        darkSystem: Boolean,
        expectedLightAppearance: Boolean,
    ) {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given bars in the opposite appearance, so that the theme has to change it
            runOnUiThread { bars().setLightAppearance(!expectedLightAppearance) }

            // When the theme chosen is drawn, in the mode of the system
            setContent {
                BloxTheme(kind = kind, darkSystem = darkSystem) {
                    BloxSystemBars(top = { null }, darkSystem = darkSystem)
                }
            }
            waitForIdle()

            // Then the status bar and the navigation bar have dark icons on a light variant, light ones on a dark one
            assertLightAppearance(expectedLightAppearance)
        }
    }

    @Test
    @DisplayName("the system bars change their appearance as soon as the variant drawn changes")
    fun theSystemBarsFollowAChangeOfVariant() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given Colorful by day: dark icons
            var darkSystem by mutableStateOf(false)
            setContent {
                BloxTheme(kind = BloxThemeKind.COLORFUL, darkSystem = darkSystem) {
                    BloxSystemBars(top = { null }, darkSystem = darkSystem)
                }
            }
            waitForIdle()
            assertLightAppearance(true)

            // When the system goes dark, in the same composition
            darkSystem = true
            waitForIdle()

            // Then light icons, on the night variant
            assertLightAppearance(false)
        }
    }

    @Test
    @DisplayName(
        "at night in the Original theme, the bars turn light under the Locksmith, and dark again once it is left",
    )
    fun theSystemBarsFollowTheLocksmith() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given the Original theme at night, on the cartridges: dark icons on its light page
            val stack = mutableStateListOf<NavKey>(CartridgesRoute)
            setContent {
                BloxTheme(kind = BloxThemeKind.ORIGINAL, darkSystem = true) {
                    BloxSystemBars(top = { stack.lastOrNull() }, darkSystem = true)
                }
            }
            waitForIdle()
            assertLightAppearance(true)

            // When the Locksmith opens, in its Colorful night variant
            stack.add(LocksmithRoute(level = 1, seed = 1))
            waitForIdle()

            // Then light icons on its dark page, and dark ones again once back on the cartridges
            assertLightAppearance(false)
            stack.removeAt(stack.lastIndex)
            waitForIdle()
            assertLightAppearance(true)
        }
    }

    private fun AndroidComposeUiTest<ComponentActivity>.bars(): WindowInsetsControllerCompat {
        val window = checkNotNull(activity).window
        return WindowCompat.getInsetsController(window, window.decorView)
    }

    private fun AndroidComposeUiTest<ComponentActivity>.assertLightAppearance(expected: Boolean) {
        runOnUiThread {
            val bars = bars()
            assertThat(bars.isAppearanceLightStatusBars).isEqualTo(expected)
            assertThat(bars.isAppearanceLightNavigationBars).isEqualTo(expected)
        }
    }

    private fun WindowInsetsControllerCompat.setLightAppearance(light: Boolean) {
        isAppearanceLightStatusBars = light
        isAppearanceLightNavigationBars = light
    }
}
