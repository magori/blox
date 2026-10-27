package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class NameFieldFrameTest {
    @Test
    fun `the name field is framed in ink while it has the focus, in danger when refused, and not framed otherwise`() {
        // Given
        val palette = BloxPalettes.ColorfulLight

        // When
        val frames = listOf(false to false, true to false, false to true, true to true).map { (focused, refused) ->
            nameFieldFrame(focused = focused, refused = refused, palette = palette)
        }

        // Then a refused name in danger, focused or not
        assertThat(frames).containsExactly(null, palette.ink, palette.danger, palette.danger)
    }
}
