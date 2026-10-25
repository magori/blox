package ch.magori.blox.theme

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.core.view.WindowCompat
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * The real appearance of the system bars, read from the window, for the variant drawn. The mode of the system is
 * passed to [BloxTheme] (`darkSystem`): the test does not depend on the mode of the device, nor changes it.
 */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
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
            // Given the theme chosen, in the mode of the system
            setContent {
                BloxTheme(kind = kind, darkSystem = darkSystem) { BloxSystemBars() }
            }
            waitForIdle()

            // Then the status bar and the navigation bar have dark icons on a light variant, light ones on a dark one
            runOnUiThread {
                val window = checkNotNull(activity).window
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                assertThat(controller.isAppearanceLightStatusBars).isEqualTo(expectedLightAppearance)
                assertThat(controller.isAppearanceLightNavigationBars).isEqualTo(expectedLightAppearance)
            }
        }
    }
}
