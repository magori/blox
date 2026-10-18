package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.cellsOf
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Ticks
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassicEntryDelayTest {
    private val entryDelay: EntryDelay = ClassicEntryDelay

    @ParameterizedTest(name = "pivot locked on row {0} -> 10 ticks")
    @CsvSource("19", "18")
    fun `a piece locked in the two bottom rows waits 10 ticks before the next one appears`(pivotRow: Int) {
        assertThat(ticksAfterLockOn(pivotRow)).isEqualTo(Ticks(10))
    }

    @ParameterizedTest(name = "pivot locked on row {0} -> {1} ticks")
    @CsvSource("17, 12", "14, 12", "13, 14", "10, 14", "9, 16", "6, 16", "5, 18", "2, 18")
    fun `each group of 4 rows higher adds 2 ticks`(
        pivotRow: Int,
        expectedTicks: Int,
    ) {
        assertThat(ticksAfterLockOn(pivotRow)).isEqualTo(Ticks(expectedTicks))
    }

    @ParameterizedTest(name = "pivot locked on row {0} -> 18 ticks")
    @CsvSource("1", "0", "-1", "-2")
    fun `the entry delay is at most 18 ticks`(pivotRow: Int) {
        assertThat(ticksAfterLockOn(pivotRow)).isEqualTo(Ticks(18))
    }

    @ParameterizedTest(name = "pivot on row {0} is refused")
    @CsvSource("-3", "20")
    fun `a row outside -2 to 19 is refused`(pivotRow: Int) {
        assertThatIllegalArgumentException()
            .isThrownBy { ticksAfterLockOn(pivotRow) }
            .withMessageContaining("pivot row must be in -2..19: $pivotRow")
    }

    /** When: a piece locks with its pivot on [pivotRow]; the ticks before the next piece appears. */
    private fun ticksAfterLockOn(pivotRow: Int): Ticks = entryDelay.ticksAfterLock(pieceWithPivotOn(pivotRow))

    private companion object {
        val T_UP = Orientation(
            cellsOf(
                """
                .T.
                T@T
                """,
            ),
        )

        fun pieceWithPivotOn(row: Int): Piece = Piece(PieceType.T, T_UP, Position(column = 4, row = row))
    }
}
