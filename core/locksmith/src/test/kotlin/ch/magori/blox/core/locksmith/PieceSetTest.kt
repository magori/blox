package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PieceSetTest {
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource("ALL, IOTSZJL", "WITHOUT_S_Z, IOTJL", "S_Z_T, TSZ")
    fun `a set draws its types in the order of the piece types, whatever was placed`(
        set: String,
        types: String,
    ) {
        // Given: an I and two T already placed
        val placed = listOf(PieceType.I, PieceType.T, PieceType.T)

        // When
        val drawable = PieceSet.valueOf(set).drawable(placed)

        // Then
        assertThat(drawable).isEqualTo(types.map { PieceType.valueOf(it.toString()) })
    }

    @Test
    fun `one of each draws the types not placed yet`() {
        // Given
        val placed = listOf(PieceType.I, PieceType.L, PieceType.S)

        // When
        val drawable = PieceSet.ONE_OF_EACH.drawable(placed)

        // Then
        assertThat(drawable).containsExactly(PieceType.O, PieceType.T, PieceType.Z, PieceType.J)
    }
}
