package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class ScriptedRandomizerTest {
    @Test
    fun `a scripted randomizer deals its pieces once, then a further draw fails clearly`() {
        // Given
        val randomizer = ScriptedRandomizer(PieceType.T, PieceType.I)

        // When
        val dealt = listOf(randomizer.next(previous = null), randomizer.next(previous = PieceType.T))

        // Then
        assertThat(dealt).containsExactly(PieceType.T, PieceType.I)
        assertThatIllegalStateException()
            .isThrownBy { randomizer.next(previous = PieceType.I) }
            .withMessage("Only 2 pieces were scripted")
    }

    @Test
    fun `a cycling randomizer deals its pieces again and again`() {
        // Given
        val randomizer = ScriptedRandomizer.cycling(PieceType.T, PieceType.I, PieceType.O)

        // When
        val dealt = (1..7).map { randomizer.next(previous = null) }

        // Then
        assertThat(dealt).containsExactly(
            PieceType.T,
            PieceType.I,
            PieceType.O,
            PieceType.T,
            PieceType.I,
            PieceType.O,
            PieceType.T,
        )
    }

    @Test
    fun `a cycling randomizer needs at least one piece`() {
        assertThatIllegalArgumentException()
            .isThrownBy { ScriptedRandomizer.cycling() }
            .withMessage("At least one piece must be scripted")
    }
}
