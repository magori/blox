package ch.magori.blox.feature.game.topbar

import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.GameUiState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ShownRecordTest {
    private val game = GameUiState(
        board = Board.empty(),
        piece = null,
        next = nextPieceCells(PieceType.I, ClassicRotationSystem),
        level = Level(5),
        lines = Lines(52),
        score = Points(12_380),
        record = 12_340,
    )

    @Test
    fun `until it is beaten the top bar shows the record of the records`() {
        // When
        val shown = game.shownRecord()

        // Then
        assertThat(shown).isEqualTo(ShownRecord(value = 12_340, beaten = false))
    }

    @Test
    fun `once beaten the record shown follows the score, as the new record`() {
        // When
        val shown = game.copy(bestBeaten = true).shownRecord()

        // Then
        assertThat(shown).isEqualTo(ShownRecord(value = 12_380, beaten = true))
    }

    @Test
    fun `a record not read yet is shown as unknown`() {
        // When
        val shown = game.copy(record = null).shownRecord()

        // Then
        assertThat(shown).isEqualTo(ShownRecord(value = null, beaten = false))
    }
}
