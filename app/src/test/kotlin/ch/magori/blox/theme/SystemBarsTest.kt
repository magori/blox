package ch.magori.blox.theme

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.daily.navigation.DailyRoute
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.garden.navigation.GardenRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.home.navigation.SettingsRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class SystemBarsTest {
    @ParameterizedTest(name = "{0} -> dark icons: {1}")
    @CsvSource(
        "ORIGINAL, true",
        "COLORFUL_LIGHT, true",
        "COLORFUL_DARK, false",
    )
    fun `the icons of the system bars are dark on a light variant and light on a dark one`(
        variant: BloxThemeVariant,
        expectedDarkIcons: Boolean,
    ) {
        assertThat(hasDarkSystemBarIcons(variant)).isEqualTo(expectedDarkIcons)
    }

    @ParameterizedTest
    @MethodSource("colorfulOnlyScreens")
    fun `the bars of a screen drawn in colorful only follow its colorful variant, whatever the theme chosen`(
        top: NavKey,
    ) {
        assertThat(COLORFUL_CASES).allSatisfy { (chosen, darkSystem, expected) ->
            assertThat(systemBarVariant(top, chosen, darkSystem)).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @MethodSource("otherScreens")
    fun `the bars of every other screen follow the theme chosen`(top: NavKey?) {
        assertThat(BloxThemeVariant.entries).allSatisfy { chosen ->
            assertThat(systemBarVariant(top, chosen, darkSystem = true)).isEqualTo(chosen)
        }
    }

    private companion object {
        /** The theme chosen and the mode of the system, and the variant of the bars of a screen drawn in colorful only. */
        val COLORFUL_CASES = listOf(
            Triple(BloxThemeVariant.ORIGINAL, false, BloxThemeVariant.COLORFUL_LIGHT),
            Triple(BloxThemeVariant.ORIGINAL, true, BloxThemeVariant.COLORFUL_DARK),
            Triple(BloxThemeVariant.COLORFUL_LIGHT, true, BloxThemeVariant.COLORFUL_DARK),
            Triple(BloxThemeVariant.COLORFUL_DARK, false, BloxThemeVariant.COLORFUL_LIGHT),
        )

        @JvmStatic
        fun colorfulOnlyScreens(): List<Arguments> = listOf(
            argumentSet("the cartridges", CartridgesRoute),
            argumentSet("the locksmith", LocksmithRoute(level = 1, seed = 1)),
            argumentSet("a daily game", GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id = 1L)),
            argumentSet("the entry of the daily seed", DailyRoute),
            argumentSet("the musical garden", GardenRoute),
        )

        @JvmStatic
        fun otherScreens(): List<Arguments> = listOf(
            argumentSet("the home", HomeRoute),
            argumentSet("the settings", SettingsRoute),
            argumentSet("a classic game", GameRoute(startLevel = 0, seed = 1L)),
            argumentSet("an empty stack", null),
        )
    }
}
