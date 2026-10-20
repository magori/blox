package ch.magori.blox.feature.game.sidepanel

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.RotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class NextPieceCellsTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "I, IIII",
        "O, OO|OO",
        "T, TTT|.T.",
        "J, JJJ|..J",
        "Z, ZZ.|.ZZ",
        "S, .SS|SS.",
        "L, LLL|L..",
    )
    fun `the next piece is its spawn orientation, its cells brought to the origin, with its size`(
        type: PieceType,
        drawing: String,
    ) {
        assertThat(nextPieceCells(type, ClassicRotationSystem)).isEqualTo(cellsDrawn(type, drawing))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(PieceType::class)
    fun `every next piece fits the preview the layout reserves, 4 cells wide and 2 high`(type: PieceType) {
        // When
        val next = nextPieceCells(type, ClassicRotationSystem)

        // Then
        assertThat(next.columns).isLessThanOrEqualTo(SidePanelSizes.PREVIEW_COLUMNS)
        assertThat(next.rows).isLessThanOrEqualTo(SidePanelSizes.PREVIEW_ROWS)
    }

    @Test
    fun `a piece reaching above its pivot is brought to the origin too`() {
        val verticalI = object : RotationSystem by ClassicRotationSystem {
            override fun spawnOrientation(type: PieceType): Orientation =
                ClassicRotationSystem.orientations(PieceType.I).first()
        }

        assertThat(nextPieceCells(PieceType.I, verticalI)).isEqualTo(cellsDrawn(PieceType.I, "I|I|I|I"))
    }

    /**
     * The cells of a piece of [type] drawn in [drawing], rows separated by `|`: `.` for an empty cell, a
     * letter for a cell of the piece.
     */
    private fun cellsDrawn(
        type: PieceType,
        drawing: String,
    ): NextPieceCells {
        val rows = drawing.split('|')
        val cells = rows.flatMapIndexed { row, line ->
            line.mapIndexedNotNull { column, symbol -> Position(column, row).takeIf { symbol != '.' } }
        }.toSet()
        return NextPieceCells(type, cells, columns = rows.first().length, rows = rows.size)
    }
}
