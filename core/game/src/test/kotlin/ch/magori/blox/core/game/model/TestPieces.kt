package ch.magori.blox.core.game.model

import ch.magori.blox.core.game.cellsOf

private val T_DOWN = Orientation(
    cellsOf(
        """
        T@T
        .T.
        """,
    ),
)

private val I_VERTICAL = Orientation(
    cellsOf(
        """
        I
        I
        @
        I
        """,
    ),
)

/** A T pointing down (its spawn orientation), its pivot [at] the given position. */
internal fun tPointingDown(at: Position): Piece = Piece(PieceType.T, T_DOWN, at)

/** A vertical I, two cells above its pivot and one below, its pivot [at] the given position. */
internal fun verticalI(at: Position): Piece = Piece(PieceType.I, I_VERTICAL, at)
