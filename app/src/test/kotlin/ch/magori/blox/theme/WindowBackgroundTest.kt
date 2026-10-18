package ch.magori.blox.theme

import androidx.compose.ui.graphics.toArgb
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WindowBackgroundTest {
    @Test
    fun `the window background is the page colour of the Original theme`() {
        // Given the colour of the window, shown before Compose draws anything
        val windowBackground = colorValue(name = "blox_window_background")

        // Then it is the page of the Original theme, the default one
        assertThat(windowBackground)
            .withFailMessage { "blox_window_background is $windowBackground, not #AARRGGBB in upper case" }
            .matches("#[0-9A-F]{8}")
        assertThat(windowBackground)
            .withFailMessage { "blox_window_background ($windowBackground) is not BloxPalettes.Original.page" }
            .isEqualTo("#%08X".format(BloxPalettes.Original.page.toArgb()))
    }

    /** The value of the colour [name] in colors.xml; [element] says so if there is no single one. */
    private fun colorValue(name: String): String =
        element(appResource("values/colors.xml"), "/resources/color[@name='$name']").textContent.trim()
}
