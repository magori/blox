package ch.magori.blox.feature.game.controls

import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class KeyGlyphTest {
    @Test
    fun `the letter keeps its size when it holds under the arrow in the key`() {
        // Given a letter of 20 sp at the normal font: 20 dp, under an arrow of 24 dp in a key of 56 dp framed by 2 dp
        val letter = 20.dp

        // When
        val height = keyLetterHeight(letter)

        // Then
        assertThat(height).isEqualTo(20.dp)
    }

    @Test
    fun `a letter exactly as high as the room under the arrow keeps its size`() {
        // Given a letter of 28 dp: 56 - 2 × 2 - 24, the room under the arrow, to the dp
        val letter = 28.dp

        // When
        val height = keyLetterHeight(letter)

        // Then
        assertThat(height).isEqualTo(28.dp)
    }

    @Test
    fun `the letter shrinks to hold under the arrow in the key when the font is enlarged`() {
        // Given the same letter, twice as large: 40 dp, where 56 - 2 × 2 - 24 = 28 dp are left
        val letter = 40.dp

        // When
        val height = keyLetterHeight(letter)

        // Then
        assertThat(height).isEqualTo(28.dp)
    }
}
