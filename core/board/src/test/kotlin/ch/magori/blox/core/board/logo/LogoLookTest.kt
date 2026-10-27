package ch.magori.blox.core.board.logo

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LogoLookTest {
    @Test
    fun `by day, Bl goes from iris-1 through iris-2 to iris-3 and X back, over a rim, a lilac and a shadow`() {
        assertThat(logoLook(night = false)).isEqualTo(
            LogoLook(
                letters = listOf(Color(0xFFC9A7FF), Color(0xFF9FF0D0), Color(0xFFFFC6A8)),
                x = listOf(Color(0xFFFFC6A8), Color(0xFFC9A7FF)),
                lettersShadows = listOf(
                    LogoShadow(Color(red = 90, green = 80, blue = 160).copy(alpha = 0.3f), drop = 12f, blur = 14f),
                    LogoShadow(Color(0xFFB48CFF), drop = 5f, blur = 0f),
                    LogoShadow(Color.White, drop = 2f, blur = 0f),
                ),
                xShadows = listOf(
                    LogoShadow(Color(red = 90, green = 80, blue = 160).copy(alpha = 0.3f), drop = 12f, blur = 14f),
                    LogoShadow(Color(0xFFB48CFF), drop = 5f, blur = 0f),
                    LogoShadow(Color.White, drop = 2f, blur = 0f),
                ),
            ),
        )
    }

    @Test
    fun `by night, Bl goes from iris-1 to iris-3 and X to the pink of the night, in a halo over a dark shadow`() {
        assertThat(logoLook(night = true)).isEqualTo(
            LogoLook(
                letters = listOf(Color(0xFFC6FF3D), Color(0xFF3DFFD0), Color(0xFFA77BFF)),
                x = listOf(Color(0xFFA77BFF), Color(0xFFFF6FB5)),
                lettersShadows = listOf(
                    LogoShadow(Color(red = 198, green = 255, blue = 61).copy(alpha = 0.45f), drop = 0f, blur = 18f),
                    LogoShadow(Color(0xFF2A2D3A), drop = 4f, blur = 0f),
                ),
                xShadows = listOf(
                    LogoShadow(Color(red = 167, green = 123, blue = 255).copy(alpha = 0.5f), drop = 0f, blur = 18f),
                    LogoShadow(Color(0xFF2A2D3A), drop = 4f, blur = 0f),
                ),
            ),
        )
    }
}
