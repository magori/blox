package ch.magori.blox.core.game.model

/**
 * The result of [Board.clearFullRows].
 *
 * @property board the board once the full rows are cleared.
 * @property clearedRows the rows cleared, as indices in the board before the clear, from top to
 * bottom; empty when no row was cleared.
 */
public data class RowsCleared(public val board: Board, public val clearedRows: List<Int>)
