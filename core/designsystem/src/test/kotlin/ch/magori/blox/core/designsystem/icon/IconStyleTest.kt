package ch.magori.blox.core.designsystem.icon

import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IconStyleTest {
    @Test
    fun `the Original theme draws its icons in pixels`() {
        assertThat(iconStyleOf(BloxThemeKind.ORIGINAL)).isEqualTo(IconStyle.PIXELS)
    }

    @Test
    fun `the Colorful theme draws its icons in strokes`() {
        assertThat(iconStyleOf(BloxThemeKind.COLORFUL)).isEqualTo(IconStyle.STROKES)
    }
}
