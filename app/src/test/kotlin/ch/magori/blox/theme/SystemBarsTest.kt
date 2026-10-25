package ch.magori.blox.theme

import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SystemBarsTest {
    @ParameterizedTest(name = "{0}, system in dark mode: {1} -> dark icons: {2}")
    @CsvSource(
        "ORIGINAL, false, true",
        "ORIGINAL, true, true",
        "COLORFUL, false, true",
        "COLORFUL, true, false",
    )
    fun `the icons of the system bars are dark on a light variant and light on a dark one, whatever the system mode`(
        kind: BloxThemeKind,
        darkSystem: Boolean,
        expectedDarkIcons: Boolean,
    ) {
        // Given the variant drawn for the theme chosen and the mode of the system
        val variant = BloxThemeVariant.of(kind, darkSystem)

        // When / Then
        assertThat(hasDarkSystemBarIcons(variant)).isEqualTo(expectedDarkIcons)
    }
}
