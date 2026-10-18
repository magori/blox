package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class LevelTest {
    @Test
    fun `a level cannot be negative`() {
        // Given a negative value
        val value = -1

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Level(value) }
    }

    @Test
    fun `level 0 is the lowest valid level`() {
        // Given the lowest value
        val value = 0

        // When
        val level = Level(value)

        // Then
        assertThat(level.value).isEqualTo(0)
    }
}
