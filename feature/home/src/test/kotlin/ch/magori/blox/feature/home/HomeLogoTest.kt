package ch.magori.blox.feature.home

import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HomeLogoTest {
    @Test
    fun `in the Original theme the logo is a square of four cells in the four greens of the pieces`() {
        assertThat(logoCells(BloxThemeVariant.ORIGINAL)).containsExactly(
            LogoCell(column = 0, row = 0, color = Color(0xFF34503A)),
            LogoCell(column = 1, row = 0, color = Color(0xFF87A67F)),
            LogoCell(column = 0, row = 1, color = Color(0xFFBFD6B6)),
            LogoCell(column = 1, row = 1, color = Color(0xFFEEF5EA)),
        )
    }

    @Test
    fun `in the Colorful theme the logo is a square of four cells in the colours of I, O, Z and S`() {
        // By day, the pastels of the pieces
        assertThat(logoCells(BloxThemeVariant.COLORFUL_LIGHT)).containsExactly(
            LogoCell(column = 0, row = 0, color = Color(0xFF8FD8FF)),
            LogoCell(column = 1, row = 0, color = Color(0xFFFFE680)),
            LogoCell(column = 0, row = 1, color = Color(0xFFFF94A6)),
            LogoCell(column = 1, row = 1, color = Color(0xFF9DEDB9)),
        )
        // By night, their neons
        assertThat(logoCells(BloxThemeVariant.COLORFUL_DARK)).containsExactly(
            LogoCell(column = 0, row = 0, color = Color(0xFF4DD8FF)),
            LogoCell(column = 1, row = 0, color = Color(0xFFFFE45E)),
            LogoCell(column = 0, row = 1, color = Color(0xFFFF4D6D)),
            LogoCell(column = 1, row = 1, color = Color(0xFF3DFF8F)),
        )
    }
}
