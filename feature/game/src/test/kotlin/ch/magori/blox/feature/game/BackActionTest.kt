package ch.magori.blox.feature.game

import ch.magori.blox.core.board.next.nextPieceCells
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.gameover.GameOverUiState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BackActionTest {
    private val playing = GameUiState(
        board = Board.empty(),
        piece = null,
        next = nextPieceCells(PieceType.T, ClassicRotationSystem),
        level = Level(0),
        lines = Lines(0),
        score = Points(0),
    )

    @Test
    fun `during the game the system back pauses it`() {
        assertThat(backAction(playing)).isEqualTo(BackAction.PAUSE)
    }

    @Test
    fun `while paused the system back resumes the game`() {
        assertThat(backAction(playing.copy(paused = true))).isEqualTo(BackAction.RESUME)
    }

    @Test
    fun `while the window is too small to play, the system back leaves the paused game, as Menu does`() {
        // Given the game paused under the message « Agrandissez la fenêtre », which Reprendre cannot leave
        val paused = playing.copy(paused = true)

        // When / Then
        assertThat(backAction(paused, tooSmallToPlay = true)).isEqualTo(BackAction.LEAVE)
    }

    @Test
    fun `once the game is over the system back leaves the game, as Menu does`() {
        // Given a game over, even one paused when it ended
        val over = GameOverUiState(
            score = Points(0),
            lines = Lines(0),
            level = Level(0),
            fourLineClears = 0,
            ticksPlayed = 0,
            newRecord = false,
        )

        // When / Then
        assertThat(backAction(playing.copy(gameOver = over))).isEqualTo(BackAction.LEAVE)
        assertThat(backAction(playing.copy(paused = true, gameOver = over))).isEqualTo(BackAction.LEAVE)
    }
}
