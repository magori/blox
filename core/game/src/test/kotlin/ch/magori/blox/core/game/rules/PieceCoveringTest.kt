package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PieceCoveringTest {
    @Test
    fun `the piece covering cells takes the orientation and the pivot whose cells are exactly those`() {
        // Given the cells of a vertical S, its pivot at (4, 3)
        val s = Piece(PieceType.S, ClassicRotationSystem.orientations(PieceType.S)[1], Position(4, 3))

        // When
        val piece = ClassicRotationSystem.pieceCovering(s.cells)

        // Then
        assertThat(piece).isEqualTo(s)
    }

    @Test
    fun `no piece covers cells that make no shape of the rotation system`() {
        // Given four cells in a row but one, a gap between them
        val cells = setOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(4, 0))

        // When
        val piece = ClassicRotationSystem.pieceCovering(cells)

        // Then
        assertThat(piece).isNull()
    }

    @Test
    fun `only the given types are tried`() {
        // Given the cells of an L
        val l = Piece(PieceType.L, ClassicRotationSystem.spawnOrientation(PieceType.L), Position(5, 5))

        // When
        val asJ = ClassicRotationSystem.pieceCovering(l.cells, types = listOf(PieceType.J))
        val asL = ClassicRotationSystem.pieceCovering(l.cells, types = listOf(PieceType.L))

        // Then
        assertThat(asJ).isNull()
        assertThat(asL).isEqualTo(l)
    }

    @Test
    fun `no piece covers no cells`() {
        // Given no cells
        val cells = emptySet<Position>()

        // When
        val piece = ClassicRotationSystem.pieceCovering(cells)

        // Then
        assertThat(piece).isNull()
    }
}
