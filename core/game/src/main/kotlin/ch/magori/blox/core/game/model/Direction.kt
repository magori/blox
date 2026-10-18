package ch.magori.blox.core.game.model

/** A one-cell move of the piece in play. There is no upward move. */
public enum class Direction(internal val offset: Position) {
    LEFT(Position(-1, 0)),
    RIGHT(Position(1, 0)),
    DOWN(Position(0, 1)),
}
