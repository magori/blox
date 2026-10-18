package ch.magori.blox.feature.game.board

import androidx.compose.ui.geometry.Offset
import ch.magori.blox.core.game.model.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BoardGeometryTest {
    @ParameterizedTest(name = "{0} × {1} gives cells of {2}")
    @CsvSource("600, 480, 24", "240, 1000, 24", "250, 500, 25", "245, 495, 24")
    fun `a cell is the smaller of the width over 10 and the height over 20, in whole pixels`(
        width: Float,
        height: Float,
        cellSize: Float,
    ) {
        // When
        val geometry = BoardGeometry.fit(width, height, COLUMNS, ROWS)

        // Then
        assertThat(geometry.cellSize).isEqualTo(cellSize)
    }

    @ParameterizedTest(name = "{0} × {1}")
    @CsvSource("245, 495", "333.3, 701.7", "1079, 1500")
    fun `a cell and the board origin are whole numbers of pixels`(
        width: Float,
        height: Float,
    ) {
        // When
        val geometry = BoardGeometry.fit(width, height, COLUMNS, ROWS)

        // Then
        assertThat(geometry.cellSize % 1f).isZero()
        assertThat(geometry.origin.x % 1f).isZero()
        assertThat(geometry.origin.y % 1f).isZero()
    }

    @ParameterizedTest(name = "{0} × {1}: cell (0, 0) at ({2}, {3}), cell (9, 19) at ({4}, {5})")
    @CsvSource("600, 480, 180, 0, 396, 456", "240, 1000, 0, 260, 216, 716", "250, 500, 0, 0, 225, 475")
    fun `the board is centred in the available space`(
        width: Float,
        height: Float,
        firstX: Float,
        firstY: Float,
        lastX: Float,
        lastY: Float,
    ) {
        // Given
        val geometry = BoardGeometry.fit(width, height, COLUMNS, ROWS)

        // When
        val topLeftCell = geometry.cellTopLeft(Position(column = 0, row = 0))
        val bottomRightCell = geometry.cellTopLeft(Position(column = 9, row = 19))

        // Then
        assertThat(topLeftCell).isEqualTo(Offset(firstX, firstY))
        assertThat(bottomRightCell).isEqualTo(Offset(lastX, lastY))
    }

    @ParameterizedTest(name = "{0} × {1}: cell (0, 0) at ({2}, {3})")
    @CsvSource("200, 50, 52, 1", "201.5, 61, 53, 7")
    fun `with the cell size of the board, a preview of 4 × 2 keeps it, centred from a whole pixel`(
        width: Float,
        height: Float,
        firstX: Float,
        firstY: Float,
    ) {
        // Given the cells of a board of 24 px, a preview of 4 × 2 cells

        // When
        val geometry = BoardGeometry.centred(width, height, columns = 4, rows = 2, cellSize = 24f)

        // Then
        assertThat(geometry.cellSize).isEqualTo(24f)
        assertThat(geometry.cellTopLeft(Position(column = 0, row = 0))).isEqualTo(Offset(firstX, firstY))
    }

    private companion object {
        const val COLUMNS = 10
        const val ROWS = 20
    }
}
