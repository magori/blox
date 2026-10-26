package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class OrientationTest {
    private val a = Position(-1, 0)
    private val b = Position(0, 0)
    private val c = Position(1, 0)
    private val d = Position(0, 1)
    private val e = Position(0, 2)

    @Test
    fun `an orientation needs exactly four distinct cells`() {
        assertThat(Orientation(setOf(a, b, c, d)).cells).containsExactlyInAnyOrder(a, b, c, d)
        assertThatIllegalArgumentException()
            .isThrownBy { Orientation(setOf(a, b, c)) }
            .withMessageContaining("4 cells")
        assertThatIllegalArgumentException()
            .isThrownBy { Orientation(setOf(a, b, c, d, e)) }
            .withMessageContaining("4 cells")
        assertThatIllegalArgumentException()
            .isThrownBy { Orientation(setOf(a, b, c, c)) }
            .withMessageContaining("4 cells")
    }

    @Test
    fun `an orientation brought to the origin touches row 0 and column 0`() {
        // Given a T reaching left of and above its pivot
        val orientation = Orientation(setOf(Position(-1, -1), Position(0, -1), Position(1, -1), Position(0, 0)))

        // When
        val cells = orientation.broughtToOrigin().cells

        // Then
        assertThat(cells.minOf(Position::column)).isZero()
        assertThat(cells.minOf(Position::row)).isZero()
    }

    @Test
    fun `bringing an orientation to the origin keeps its shape`() {
        // Given an S two columns right of and one row below the origin
        val orientation = Orientation(setOf(Position(2, 1), Position(3, 1), Position(1, 2), Position(2, 2)))

        // When
        val brought = orientation.broughtToOrigin()

        // Then the same S, each cell one column to the left and one row up
        assertThat(brought).isEqualTo(
            Orientation(setOf(Position(1, 0), Position(2, 0), Position(0, 1), Position(1, 1))),
        )
    }
}
