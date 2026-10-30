package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Rotation

/** Something that happened in the game, for the screen and the sound. Starting a game gives none. */
public sealed interface GameEvent {
    /**
     * The piece in play turned by [rotation], clockwise with A, counterclockwise with B: [piece] is the piece as it
     * turned, before the drop of that tick. A rotation refused, or one that leaves the piece as it was (the O), sends
     * none. When the turned piece locks on the same tick, it comes before [PieceLocked].
     */
    public data class PieceRotated(public val piece: Piece, public val rotation: Rotation) : GameEvent

    /**
     * [piece] could not drop any further and joined the board, where it now lies. [bySoftDrop] tells what locked it: a
     * refused step of the soft drop (true), whether or not the soft drop had dropped rows before, or the gravity
     * (false). A neutral fact, which a mode may read (the beat bonus of Au tempo, ADR 0028). It has no default, so that
     * a new way to lock a piece has to tell which it is.
     */
    public data class PieceLocked(public val piece: Piece, public val bySoftDrop: Boolean) : GameEvent

    /** The full rows of [lineClear] were cleared, once their animation was over. */
    public data class LinesCleared(public val lineClear: LineClear) : GameEvent

    /**
     * The game went up to [level], the new level, with the lines of the [LinesCleared] it follows, on the same
     * tick. Sent only when the level changes: from the level chosen at the start, the first one comes at the
     * lines the level progression of the mode says. Starting a game sends none.
     */
    public data class LevelUp(public val level: Level) : GameEvent

    /**
     * The stack entered the danger ([on] true) or left it ([on] false), as measured when a piece locked, on
     * the board once its full rows are cleared. Without a full row, it follows [PieceLocked] on the tick of
     * the lock; with full rows, it follows [LinesCleared], and [LevelUp] if there is one, at the end of their
     * animation, when the cleared board is shown.
     */
    public data class StackDanger(public val on: Boolean) : GameEvent

    /**
     * The game is over: the next piece could not appear. It is the last event of the game, sent once, on
     * the tick the state turns to [Phase.GameOver], and the only event of that tick; it is never preceded by
     * [StackDanger] off. It carries nothing: the score, lines, level, statistics and length of the game are
     * in that state.
     */
    public data object GameOver : GameEvent
}
