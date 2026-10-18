package ch.magori.blox.feature.home

import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HomeLogoTest {
    @Test
    fun `in the Original theme the logo is a square of four cells in the four greens of the pieces`() {
        assertThat(logoCells(BloxPalettes.Original)).containsExactly(
            LogoCell(column = 0, row = 0, color = Color(0xFF34503A)),
            LogoCell(column = 1, row = 0, color = Color(0xFF87A67F)),
            LogoCell(column = 0, row = 1, color = Color(0xFFBFD6B6)),
            LogoCell(column = 1, row = 1, color = Color(0xFFEEF5EA)),
        )
    }

    @Test
    fun `in the Colorful theme the logo is a square of four cells in the colours of I, O, Z and S`() {
        assertThat(logoCells(BloxPalettes.Colorful)).containsExactly(
            LogoCell(column = 0, row = 0, color = Color(0xFF3FC5E8)),
            LogoCell(column = 1, row = 0, color = Color(0xFFF2C641)),
            LogoCell(column = 0, row = 1, color = Color(0xFFE5534B)),
            LogoCell(column = 1, row = 1, color = Color(0xFF5BC46A)),
        )
    }
}
