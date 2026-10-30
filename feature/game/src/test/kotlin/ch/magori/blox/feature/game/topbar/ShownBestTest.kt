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

class ShownBestTest {
    private val game = GameUiState(
        board = Board.empty(),
        piece = null,
        next = nextPieceCells(PieceType.I, ClassicRotationSystem),
        level = Level(5),
        lines = Lines(52),
        score = Points(12_380),
        best = 12_340,
    )

    @Test
    fun `until it is beaten the top bar shows the best of the results`() {
        // When
        val shown = game.shownBest()

        // Then
        assertThat(shown).isEqualTo(ShownBest(value = 12_340, beaten = false))
    }

    @Test
    fun `once beaten the best shown follows the score, as the new best`() {
        // When
        val shown = game.copy(bestBeaten = true).shownBest()

        // Then
        assertThat(shown).isEqualTo(ShownBest(value = 12_380, beaten = true))
    }

    @Test
    fun `a best not read yet is shown as unknown`() {
        // When
        val shown = game.copy(best = null).shownBest()

        // Then
        assertThat(shown).isEqualTo(ShownBest(value = null, beaten = false))
    }
}
