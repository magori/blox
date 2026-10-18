package ch.magori.blox.core.game.model

import ch.magori.blox.core.game.cellsOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PieceTest {
    @Test
    fun `the cells of a piece are its pivot plus the offsets of its orientation`() {
        // Given a T pointing down, its pivot at (5, 0)
        val tPointingDown = Orientation(
            cellsOf(
                """
                T@T
                .T.
                """,
            ),
        )
        val piece = Piece(PieceType.T, tPointingDown, Position(5, 0))

        // When
        val cells = piece.cells

        // Then
        assertThat(cells).containsExactlyInAnyOrder(Position(4, 0), Position(5, 0), Position(6, 0), Position(5, 1))
    }
}
