package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.LineClear

/**
 * Statistics of a game, beyond its score, lines and level, which the game state holds already. The
 * length of a game is read from [GameState.tickCount]. Immutable: every operation returns new
 * statistics. Its counts can only come from those operations, never be forged.
 *
 * @property fourLineClears how many times 4 lines were cleared at once.
 */
@ConsistentCopyVisibility
public data class GameStatistics private constructor(public val fourLineClears: Int) {
    /** The statistics of a game that starts: no four-line clear. */
    public constructor() : this(fourLineClears = 0)

    /** After the lines cleared at once in [lineClear]: four lines count, any other clear changes nothing. */
    public fun afterClear(lineClear: LineClear): GameStatistics =
        if (lineClear.isFourLines) copy(fourLineClears = fourLineClears + 1) else this
}
