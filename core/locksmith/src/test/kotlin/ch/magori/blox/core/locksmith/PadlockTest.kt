package ch.magori.blox.core.locksmith

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
import org.junit.jupiter.params.provider.ValueSource

class PadlockTest {
    @Test
    fun `the hole is the union of the cells of the solution`() {
        // Given
        val solution = listOf(
            piece(PieceType.O, column = 2, row = 1),
            piece(PieceType.I, column = 5, row = 1),
            piece(PieceType.T, column = 4, row = 2),
        )

        // When
        val padlock = Padlock(solution)

        // Then
        assertThat(padlock.hole).containsExactlyInAnyOrder(
            Position(1, 1), Position(2, 1), Position(1, 2), Position(2, 2),
            Position(3, 1), Position(4, 1), Position(5, 1), Position(6, 1),
            Position(3, 2), Position(4, 2), Position(5, 2), Position(4, 3),
        )
    }

    @Test
    fun `two pieces of a solution cannot overlap`() {
        // Given: the T shares cell (4, 1) with the I
        val solution = listOf(
            piece(PieceType.O, column = 2, row = 1),
            piece(PieceType.I, column = 5, row = 1),
            piece(PieceType.T, column = 4, row = 1),
        )

        // Then
        assertThatIllegalArgumentException().isThrownBy { Padlock(solution) }.withMessageContaining("overlap")
    }

    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [2, 9])
    fun `a padlock holds 3 to 8 pieces`(count: Int) {
        // Given: flat I pieces stacked from row 1, each one on the one above
        val solution = stackedIs(count)

        // Then
        assertThatIllegalArgumentException().isThrownBy { Padlock(solution) }.withMessageContaining("3 to 8")
    }

    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [3, 8])
    fun `a padlock of 3 or 8 pieces is accepted`(count: Int) {
        // Given
        val solution = stackedIs(count)

        // When
        val padlock = Padlock(solution)

        // Then
        assertThat(padlock.hole).hasSize(4 * count)
    }

    @Test
    fun `a piece of a solution takes a Classic orientation of its type`() {
        // Given: a T drawn with the cells of a flat I
        val solution = listOf(
            piece(PieceType.O, column = 2, row = 1),
            piece(PieceType.I, column = 5, row = 1),
            Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.I), Position(5, 2)),
        )

        // Then
        assertThatIllegalArgumentException().isThrownBy { Padlock(solution) }.withMessageContaining("orientation")
    }

    @ParameterizedTest(name = "shifted by column {0}, row {1}")
    @CsvSource("-1, 0", "0, -1", "3, 0", "0, 6")
    fun `the hole stays inside the outer ring`(
        columns: Int,
        rows: Int,
    ) {
        // Given: a solution over columns 1 to 6 and rows 1 to 3, shifted onto column 0 or 9, row 0 or 9
        val solution = listOf(
            piece(PieceType.O, column = 2 + columns, row = 1 + rows),
            piece(PieceType.I, column = 5 + columns, row = 1 + rows),
            piece(PieceType.T, column = 4 + columns, row = 2 + rows),
        )

        // Then
        assertThatIllegalArgumentException().isThrownBy { Padlock(solution) }.withMessageContaining("outer ring")
    }

    @ParameterizedTest
    @MethodSource("detachedSolutions")
    fun `every piece but the first touches an earlier one edge to edge`(solution: List<Piece>) {
        // Given: a solution where a piece touches no earlier one by an edge

        // Then
        assertThatIllegalArgumentException().isThrownBy { Padlock(solution) }.withMessageContaining("edge to edge")
    }

    @Test
    fun `a piece may touch any earlier piece, not only the one before it`() {
        // Given: the second O touches the first O, not the I placed between them
        val solution = listOf(
            piece(PieceType.O, column = 2, row = 1),
            piece(PieceType.I, column = 5, row = 1),
            piece(PieceType.O, column = 2, row = 3),
        )

        // When
        val padlock = Padlock(solution)

        // Then
        assertThat(padlock.hole).hasSize(12)
    }

    @Test
    fun `the pieces given to the player follow the order I O T S Z J L, not the solution`() {
        // Given: a solution placed J, then T, then S, an order that neither PieceType nor the alphabet gives
        val padlock = Padlock.parse(
            """
            ##########
            #111######
            ###1222###
            #####233##
            #####33###
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        // When
        val given = padlock.givenPieces

        // Then
        assertThat(given).containsExactly(PieceType.T, PieceType.S, PieceType.J)
    }

    @Test
    fun `two padlocks with the same solution are equal`() {
        // Given
        val solution = stackedIs(3)

        // Then
        assertThat(Padlock(solution)).isEqualTo(Padlock(solution.toList()))
    }

    @Test
    fun `parse reads the hole and the pieces of the solution`() {
        // When: metal is #, each piece of the solution is written with its rank
        val padlock = Padlock.parse(PADLOCK)

        // Then
        assertThat(padlock.solution).containsExactly(
            piece(PieceType.O, column = 2, row = 1),
            piece(PieceType.I, column = 5, row = 1),
            piece(PieceType.T, column = 4, row = 2),
        )
        assertThat(padlock.hole).hasSize(12).contains(Position(4, 3)).doesNotContain(Position(5, 3))
    }

    @Test
    fun `parse reads the ranks of a padlock of 8 pieces`() {
        // Given: eight flat I written 1 to 8
        val solution = stackedIs(8)

        // When
        val padlock = Padlock.parse(asciiOf(solution))

        // Then
        assertThat(padlock.solution).isEqualTo(solution)
    }

    @ParameterizedTest
    @MethodSource("everyClassicPiece")
    fun `parse finds the orientation and the pivot of every Classic piece`(piece: Piece) {
        // Given: the piece first, two flat I below it, each touching the one above
        val bottom = piece.cells.maxOf(Position::row)
        val column = piece.cells.filter { it.row == bottom }.minOf(Position::column)
        val solution = listOf(piece, flatI(column, bottom + 1), flatI(column, bottom + 2))

        // When
        val padlock = Padlock.parse(asciiOf(solution))

        // Then
        assertThat(padlock.solution.first()).isEqualTo(piece)
    }

    @ParameterizedTest
    @MethodSource("malformedPadlocks")
    fun `parse rejects a grid not 10 by 10, an unknown cell, a wrong or missing rank, a rank not a piece`(
        ascii: String,
    ) {
        assertThatIllegalArgumentException().isThrownBy { Padlock.parse(ascii) }
    }

    private companion object {
        val PADLOCK =
            """
            ##########
            #112222###
            #11333####
            ####3#####
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """.trimIndent()

        @JvmStatic
        fun detachedSolutions(): List<Arguments> = listOf(
            argumentSet(
                "the T meets the O by a corner only",
                listOf(
                    piece(PieceType.O, column = 2, row = 1),
                    piece(PieceType.T, column = 4, row = 3),
                    piece(PieceType.I, column = 5, row = 5),
                ),
            ),
            argumentSet(
                "the I touches no earlier piece, the T after it touches the I",
                listOf(
                    piece(PieceType.O, column = 2, row = 1),
                    piece(PieceType.I, column = 6, row = 5),
                    piece(PieceType.T, column = 5, row = 3),
                ),
            ),
        )

        /** The 19 Classic pieces, each with its cells starting at column 1, row 1. */
        @JvmStatic
        fun everyClassicPiece(): List<Arguments> = PieceType.entries.flatMap { type ->
            ClassicRotationSystem.orientations(type).mapIndexed { index, orientation ->
                val pivot = Position(
                    1 - orientation.cells.minOf(Position::column),
                    1 - orientation.cells.minOf(Position::row),
                )
                argumentSet("$type, orientation $index", Piece(type, orientation, pivot))
            }
        }

        @JvmStatic
        fun malformedPadlocks(): List<Arguments> = listOf(
            argumentSet("9 rows", PADLOCK.lines().dropLast(1).joinToString("\n")),
            argumentSet("a row of 11 cells", PADLOCK.replaceFirst("##########", "###########")),
            argumentSet("an unknown character", PADLOCK.replaceFirst("##########", "#########.")),
            argumentSet("rank 0", PADLOCK.replace('3', '0')),
            argumentSet("rank 9", PADLOCK.replace('3', '9')),
            argumentSet("rank 3 missing", PADLOCK.replace('3', '4')),
            argumentSet("a rank of two cells", PADLOCK.replace("2222", "22##")),
        )

        /** [count] flat I pieces in columns 1 to 4, from row 1 down. */
        fun stackedIs(count: Int): List<Piece> = (1..count).map { row -> flatI(column = 1, row = row) }

        /** A flat I whose cells start at [column], on [row]. */
        fun flatI(
            column: Int,
            row: Int,
        ): Piece = piece(PieceType.I, column = column + 2, row = row)
    }
}

/** A piece in its spawn orientation, its pivot on column [column], row [row]. */
private fun piece(
    type: PieceType,
    column: Int,
    row: Int,
): Piece = Piece(type, ClassicRotationSystem.spawnOrientation(type), Position(column, row))
