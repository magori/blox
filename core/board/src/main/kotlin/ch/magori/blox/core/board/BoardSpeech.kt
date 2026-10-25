package ch.magori.blox.core.board

import ch.magori.blox.core.game.model.Board

/** What TalkBack says of the board. */
internal sealed interface BoardSpeech {
    /** Nothing on the board. */
    data object Empty : BoardSpeech

    /** A stack [height] rows high, out of the [rows] of the board. */
    data class Stack(val height: Int, val rows: Int) : BoardSpeech
}

/** What TalkBack says of [board]: the height of its stack, or that it is empty. */
internal fun boardSpeech(board: Board): BoardSpeech = if (board.stackHeight == 0) {
    BoardSpeech.Empty
} else {
    BoardSpeech.Stack(height = board.stackHeight, rows = board.rows)
}
