package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.model.verticalI
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class RotationOnBoardTest {
    private val rotationSystem: RotationSystem = ClassicRotationSystem

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("buttons")
    fun `a rotation against a wall is refused, without any kick`(rotation: Rotation) {
        // Given a vertical I in column 0: lying flat it would cover columns -2 to 1
        val piece = verticalI(at = Position(0, 10))

        // When
        val candidates = rotationSystem.candidates(piece, rotation)
        val rotated = Board.empty().turn(piece, rotation)

        // Then the Classic mode tries a single candidate, which does not fit
        assertThat(candidates).hasSize(1)
        assertThat(rotated).isNull()
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("buttons")
    fun `a rotation onto an occupied cell is refused`(rotation: Rotation) {
        // Given a T pointing down at (5, 18): turned either way, it needs the occupied cell (5, 17)
        val board = Board.parse(
            """
            .....I....
            ..........
            ..........
            """,
        )
        val piece = tPointingDown(at = Position(5, 18))

        // When
        val rotated = board.turn(piece, rotation)

        // Then
        assertThat(rotated).isNull()
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("buttons")
    fun `a rotation into the hidden rows is accepted, the pivot stays put`(rotation: Rotation) {
        // Given an I lying flat at spawn on row 0: upright it covers rows -2 to 1
        val piece = Piece(PieceType.I, rotationSystem.spawnOrientation(PieceType.I), Position(5, 0))

        // When
        val rotated = Board.empty().turn(piece, rotation)

        // Then
        assertThat(rotated).isEqualTo(verticalI(at = Position(5, 0)))
    }

    /** As the engine will do it: the first candidate that fits, or `null`. */
    private fun Board.turn(
        piece: Piece,
        rotation: Rotation,
    ): Piece? = rotationSystem.candidates(piece, rotation).firstNotNullOfOrNull(::place)

    private companion object {
        @JvmStatic
        fun buttons(): List<Arguments> = listOf(
            argumentSet("A (clockwise)", Rotation.CLOCKWISE),
            argumentSet("B (counterclockwise)", Rotation.COUNTERCLOCKWISE),
        )
    }
}
