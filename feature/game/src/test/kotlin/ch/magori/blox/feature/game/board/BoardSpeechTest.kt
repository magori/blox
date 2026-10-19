package ch.magori.blox.feature.game.board

import ch.magori.blox.core.game.model.Board
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BoardSpeechTest {
    @Test
    fun `TalkBack says the height of the stack, holes included, out of the rows of the board`() {
        // Given a stack 3 rows high, with a hole under its top
        val board = Board.parse(
            """
            ....I.....
            ..........
            JJJ..OO..L
            """,
        )

        // When / Then
        assertThat(boardSpeech(board)).isEqualTo(BoardSpeech.Stack(height = 3, rows = 20))
    }

    @Test
    fun `TalkBack says an empty board is empty, rather than a stack of no row`() {
        // When / Then
        assertThat(boardSpeech(Board.empty())).isEqualTo(BoardSpeech.Empty)
    }
}
