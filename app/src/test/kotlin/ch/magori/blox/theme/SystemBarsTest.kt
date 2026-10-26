package ch.magori.blox.theme

import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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

    @ParameterizedTest(name = "chosen {0}, system dark {1} -> {2}")
    @CsvSource(
        "ORIGINAL, false, COLORFUL_LIGHT",
        "ORIGINAL, true, COLORFUL_DARK",
        "COLORFUL_LIGHT, true, COLORFUL_DARK",
        "COLORFUL_DARK, false, COLORFUL_LIGHT",
    )
    fun `the bars of the locksmith follow its colorful variant, whatever the theme chosen`(
        chosen: BloxThemeVariant,
        darkSystem: Boolean,
        expected: BloxThemeVariant,
    ) {
        assertThat(systemBarVariant(LocksmithRoute(level = 1, seed = 1), chosen, darkSystem)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "chosen {0}")
    @CsvSource("ORIGINAL", "COLORFUL_LIGHT", "COLORFUL_DARK")
    fun `the bars of every other screen follow the theme chosen`(chosen: BloxThemeVariant) {
        assertThat(systemBarVariant(CartridgesRoute, chosen, darkSystem = true)).isEqualTo(chosen)
        assertThat(systemBarVariant(null, chosen, darkSystem = true)).isEqualTo(chosen)
    }
}
