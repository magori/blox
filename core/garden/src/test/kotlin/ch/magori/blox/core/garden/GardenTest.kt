package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class GardenTest {
    @Test
    fun `a piece inside the grid on free cells is placed`() {
        // Given: an O on (2, 4) to (3, 5)
        val placed = piece(PieceType.O, column = 3, row = 4)
        val garden = Garden.EMPTY.placed(placed)

        // When: a T pointing down on (4, 5) to (6, 5) and (5, 6), right against the O
        val piece = piece(PieceType.T, column = 5, row = 5)
        val placement = garden.place(piece)

        // Then
        assertThat(placement).isInstanceOf(GardenPlacement.Placed::class.java)
        assertThat((placement as GardenPlacement.Placed).garden.pieces).containsExactlyInAnyOrder(placed, piece)
        assertThat(garden.pieces).containsExactly(placed)
    }

    @ParameterizedTest(name = "pivot ({0}, {1})")
    @CsvSource("0, 5", "15, 5", "5, -1", "5, 13")
    fun `a piece off the grid is refused`(
        column: Int,
        row: Int,
    ) {
        // Given: a T pointing down, one of its cells one step off the 16 × 14 grid
        val piece = piece(PieceType.T, column, row)

        // When
        val placement = Garden.EMPTY.place(piece)

        // Then
        assertThat(placement).isEqualTo(GardenPlacement.Refused(GardenRefusal.OUT_OF_GRID))
    }

    @Test
    fun `a piece over a placed one is refused as an overlap`() {
        // Given: an O on (2, 4) to (3, 5)
        val garden = Garden.EMPTY.placed(piece(PieceType.O, column = 3, row = 4))

        // When: a T pointing down whose leftmost cell is (3, 5)
        val placement = garden.place(piece(PieceType.T, column = 4, row = 5))

        // Then
        assertThat(placement).isEqualTo(GardenPlacement.Refused(GardenRefusal.OVERLAP))
    }

    @Test
    fun `a piece both off the grid and over a placed one is refused as off the grid`() {
        // Given: an O in the bottom left corner, on (0, 12) to (1, 13)
        val garden = Garden.EMPTY.placed(piece(PieceType.O, column = 1, row = 12))

        // When: a T pointing down on (-1, 13) to (1, 13) and (0, 14), over the O and off the grid
        val placement = garden.place(piece(PieceType.T, column = 0, row = 13))

        // Then
        assertThat(placement).isEqualTo(GardenPlacement.Refused(GardenRefusal.OUT_OF_GRID))
    }

    @ParameterizedTest(name = "pivot ({0}, {1})")
    @CsvSource("1, 0", "14, 12")
    fun `a piece along the edges of the grid is placed`(
        column: Int,
        row: Int,
    ) {
        // Given: a T pointing down in the top left or the bottom right corner of the 16 × 14 grid
        val piece = piece(PieceType.T, column, row)

        // When
        val placement = Garden.EMPTY.place(piece)

        // Then
        assertThat(placement).isInstanceOf(GardenPlacement.Placed::class.java)
    }

    @Test
    fun `a piece takes a Classic orientation of its type`() {
        // Given: a T drawn with the cells of a flat I
        val piece = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.I), Position(5, 5))

        // Then
        assertThatIllegalArgumentException().isThrownBy { Garden.EMPTY.place(piece) }.withMessageContaining("Classic")
    }

    @Test
    fun `the piece placed on a cell is found there`() {
        // Given: an O on (2, 4) to (3, 5) and a T pointing down on (5, 5) to (7, 5) and (6, 6)
        val o = piece(PieceType.O, column = 3, row = 4)
        val t = piece(PieceType.T, column = 6, row = 5)
        val garden = Garden.EMPTY.placed(o).placed(t)

        // Then
        assertThat(garden.pieceAt(Position(6, 6))).isEqualTo(t)
        assertThat(garden.pieceAt(Position(2, 5))).isEqualTo(o)
        assertThat(garden.pieceAt(Position(4, 4))).isNull()
    }

    @Test
    fun `a placed piece is taken back`() {
        // Given
        val o = piece(PieceType.O, column = 3, row = 4)
        val t = piece(PieceType.T, column = 6, row = 5)
        val garden = Garden.EMPTY.placed(o).placed(t)

        // When
        val takenBack = garden.takeBack(t)

        // Then: its cells are free again, the other piece stays
        assertThat(takenBack.pieces).containsExactly(o)
        assertThat(takenBack.pieceAt(Position(6, 6))).isNull()
        assertThat(takenBack.place(t)).isEqualTo(GardenPlacement.Placed(garden))
    }

    @Test
    fun `clearing the garden leaves it empty`() {
        // Given
        val garden = Garden.EMPTY
            .placed(piece(PieceType.O, column = 3, row = 4))
            .placed(piece(PieceType.T, column = 6, row = 5))

        // When
        val cleared = garden.clear()

        // Then
        assertThat(cleared).isEqualTo(Garden.EMPTY)
        assertThat(cleared.pieces).isEmpty()
    }

    @Test
    fun `parse reads the pieces of a garden`() {
        // When: a free cell is `.`, each piece is written with the letter of its type
        val garden = Garden.parse(
            """
            ................
            .OO.............
            .OO......I......
            .........I......
            ....TTT..I......
            .....T...I......
            ................
            ................
            ................
            ................
            ................
            ................
            .............LLL
            .............L..
            """,
        )

        // Then
        assertThat(garden.pieces).containsExactlyInAnyOrder(
            piece(PieceType.O, column = 2, row = 1),
            piece(PieceType.I, column = 9, row = 4, orientation = ClassicRotationSystem.orientations(PieceType.I)[0]),
            piece(PieceType.T, column = 5, row = 4),
            piece(PieceType.L, column = 14, row = 12),
        )
    }

    @Test
    fun `parse reads apart two pieces of the same type that do not touch`() {
        // When
        val garden = Garden.parse(TWO_TS)

        // Then
        assertThat(garden.pieces).containsExactlyInAnyOrder(
            piece(PieceType.T, column = 1, row = 0),
            piece(PieceType.T, column = 7, row = 0),
        )
    }

    @Test
    fun `parse tells apart two touching pieces of the same type when one is written with a digit`() {
        // When: the second T touches the first one, it is written with the digit 1, its type read from its shape
        val garden = Garden.parse(
            topRows(
                "TTT111..........",
                ".T..1...........",
            ),
        )

        // Then
        assertThat(garden.pieces).containsExactlyInAnyOrder(
            piece(PieceType.T, column = 1, row = 0),
            piece(PieceType.T, column = 4, row = 0),
        )
    }

    @ParameterizedTest
    @MethodSource("malformedGardens")
    fun `parse rejects a grid not 16 by 14, an unknown mark, cells that make no piece of their mark`(
        ascii: String,
        message: String,
    ) {
        assertThatIllegalArgumentException().isThrownBy { Garden.parse(ascii) }.withMessageContaining(message)
    }

    @Test
    fun `taking back a piece that is not placed leaves the garden as is`() {
        // Given: a flat S
        val s = piece(PieceType.S, column = 6, row = 5)
        val garden = Garden.EMPTY.placed(s)

        // When: a T placed nowhere, then a Z drawn on the very cells of the S
        val absent = garden.takeBack(piece(PieceType.T, column = 2, row = 2))
        val otherType = garden.takeBack(Piece(PieceType.Z, s.orientation, s.pivot))

        // Then
        assertThat(absent).isEqualTo(garden)
        assertThat(otherType).isEqualTo(garden)
    }

    @ParameterizedTest
    @MethodSource("everyClassicPiece")
    fun `parse reads back every Classic piece placed in a garden, by its letter or by a digit`(piece: Piece) {
        // Given
        val garden = Garden.EMPTY.placed(piece)

        // When
        val byLetter = Garden.parse(ascii(garden, mark = piece.type.name.single()))
        val byDigit = Garden.parse(ascii(garden, mark = '1'))

        // Then
        assertThat(byLetter).isEqualTo(garden)
        assertThat(byDigit).isEqualTo(garden)
    }

    @Test
    fun `an empty garden is calm`() {
        // Given / When
        val garden = Garden.EMPTY

        // Then
        assertThat(garden.mood).isEqualTo(GardenMood.CALM)
    }

    @Test
    fun `changing the mood keeps the pieces where they are`() {
        // Given: a calm garden with an O and a T
        val garden = Garden.EMPTY
            .placed(piece(PieceType.O, column = 3, row = 4))
            .placed(piece(PieceType.T, column = 6, row = 5))

        // When
        val epic = garden.withMood(GardenMood.EPIC)

        // Then: the same cells, heard in the other scale
        assertThat(epic.mood).isEqualTo(GardenMood.EPIC)
        assertThat(epic.pieces).isEqualTo(garden.pieces)
        assertThat(garden.mood).isEqualTo(GardenMood.CALM)
    }

    @Test
    fun `placing or taking back a piece keeps the mood`() {
        // Given: a mysterious garden
        val garden = Garden.EMPTY.withMood(GardenMood.MYSTERIOUS)
        val o = piece(PieceType.O, column = 3, row = 4)

        // When
        val placed = garden.placed(o)
        val takenBack = placed.takeBack(o)

        // Then
        assertThat(placed.mood).isEqualTo(GardenMood.MYSTERIOUS)
        assertThat(takenBack.mood).isEqualTo(GardenMood.MYSTERIOUS)
    }

    @Test
    fun `clearing the garden keeps its mood`() {
        // Given: a joyful garden with an O
        val garden = Garden.EMPTY.withMood(GardenMood.JOYFUL).placed(piece(PieceType.O, column = 3, row = 4))

        // When
        val cleared = garden.clear()

        // Then
        assertThat(cleared).isEqualTo(Garden.EMPTY.withMood(GardenMood.JOYFUL))
    }

    private fun Garden.placed(piece: Piece): Garden = (place(piece) as GardenPlacement.Placed).garden

    private companion object {
        /** The 19 Classic pieces, each with its pivot on (7, 6), in the middle of the grid. */
        @JvmStatic
        fun everyClassicPiece(): List<Arguments> = PieceType.entries.flatMap { type ->
            ClassicRotationSystem.orientations(type).mapIndexed { index, orientation ->
                argumentSet("$type, orientation $index", piece(type, column = 7, row = 6, orientation = orientation))
            }
        }

        /** [garden] written as [Garden.parse] reads it, every piece with [mark]. */
        fun ascii(
            garden: Garden,
            mark: Char,
        ): String = (0 until Garden.ROWS).joinToString("\n") { row ->
            (0 until Garden.COLUMNS).joinToString("") { column ->
                if (garden.pieceAt(Position(column, row)) == null) "." else "$mark"
            }
        }

        val TWO_TS = topRows(
            "TTT...TTT.......",
            ".T.....T........",
        )

        @JvmStatic
        fun malformedGardens(): List<Arguments> = listOf(
            argumentSet("13 rows", TWO_TS.lines().dropLast(1).joinToString("\n"), "14 rows, got 13"),
            argumentSet("15 rows", TWO_TS + "\n" + ".".repeat(Garden.COLUMNS), "14 rows, got 15"),
            argumentSet(
                "a row of 15 cells",
                TWO_TS.replaceFirst(".T.....T........", ".T.....T......."),
                "row 1 has 15 cells",
            ),
            argumentSet(
                "a row of 17 cells",
                TWO_TS.replaceFirst("TTT...TTT.......", "TTT...TTT........"),
                "row 0 has 17 cells",
            ),
            argumentSet("an unknown mark", TWO_TS.replaceFirst("TTT...", "TTT..#"), "Unknown mark '#' at (5, 0)"),
            argumentSet(
                "a letter on the cells of another type",
                TWO_TS.replace('T', 'L'),
                "The cells marked 'L' from (0, 0) make no L",
            ),
            argumentSet(
                "two touching pieces of the same letter",
                TWO_TS.replace("TTT...TTT", "TTTTTT...").replace(".T.....T.", ".T..T...."),
                "The cells marked 'T' from (0, 0) make no T",
            ),
            argumentSet(
                "a digit on five cells",
                TWO_TS.replace("TTT...TTT.......", "TTT...TTT..11111"),
                "The cells marked '1' from (11, 0) make no Classic piece",
            ),
        )

        /** A garden whose top rows are [rows], the rows below free. */
        fun topRows(vararg rows: String): String =
            (rows.toList() + List(Garden.ROWS - rows.size) { ".".repeat(Garden.COLUMNS) }).joinToString("\n")
    }
}

/** A piece in [orientation], its spawn one unless told, its pivot on column [column], row [row]. */
private fun piece(
    type: PieceType,
    column: Int,
    row: Int,
    orientation: Orientation = ClassicRotationSystem.spawnOrientation(type),
): Piece = Piece(type, orientation, Position(column, row))
