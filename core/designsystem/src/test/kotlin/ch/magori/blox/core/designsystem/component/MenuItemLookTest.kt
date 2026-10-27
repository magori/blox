package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class MenuItemLookTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("colorful")
    fun `a deleting item is in danger, icon and label`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When an item that deletes, at rest on the panel of the menu
        val look = menuItemLook(palette, danger = true, pressed = false)

        // Then its icon and its label, both in its ink, are in danger, readable on the panel (WCAG 2.1, 1.4.3)
        assertThat(look.ink).isEqualTo(palette.danger)
        assertThat(contrast(look.ink, palette.panel)).isGreaterThanOrEqualTo(4.5)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("colorful")
    fun `a pressed item takes button pressed, a deleting one danger, their ink standing out by 4,5 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When pressed
        val item = menuItemLook(palette, danger = false, pressed = true)
        val deleting = menuItemLook(palette, danger = true, pressed = true)

        // Then as a pressed secondary button, and as a pressed danger button
        assertThat(item).isEqualTo(MenuItemLook(fill = palette.buttonPressed, ink = palette.buttonInk))
        assertThat(deleting).isEqualTo(MenuItemLook(fill = palette.danger, ink = palette.dangerInk))
        assertThat(contrast(item.ink, item.fill)).isGreaterThanOrEqualTo(4.5)
        assertThat(contrast(deleting.ink, deleting.fill)).isGreaterThanOrEqualTo(4.5)
    }

    companion object {
        @JvmStatic
        fun colorful(): List<Arguments> = listOf(
            Arguments.of("Couleurs, jour", BloxPalettes.ColorfulLight),
            Arguments.of("Couleurs, nuit", BloxPalettes.ColorfulDark),
        )
    }
}
