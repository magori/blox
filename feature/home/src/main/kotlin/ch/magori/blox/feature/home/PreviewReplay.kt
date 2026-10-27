package ch.magori.blox.feature.home

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicGravityTable
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.ClassicScoringRule

/**
 * The preview of the home (P5.77, V27), pure and counted in ticks (ADR 0006): a T comes in at the top of the rows shown
 * ([PREVIEW_ROWS]) and falls at the gravity of [level] (`ClassicGravityTable`), locks on the gravity tick after it has
 * landed, as in a game, and fills the two holes of the stack: its two lines clear, scored as the Classic mode scores
 * them (`ClassicScoringRule`). They show cleared, with their points, for 2 seconds; then the T comes in again.
 * [elapsed] counts the ticks since the T came in.
 */
internal data class PreviewReplay(val level: Level, val elapsed: Int = 0) {
    private val ticksPerRow: Int get() = ticksPerRow(level)

    /** When the T locks: the gravity tick after it landed finds the floor. */
    private val lockTick: Int get() = lockTick(level)

    private val locked: Boolean get() = elapsed >= lockTick

    /** This preview [ticks] later: once its lines have shown cleared for [SHOWN_CLEARED] ticks, it plays again. */
    fun advancedBy(ticks: Int): PreviewReplay = copy(elapsed = (elapsed + ticks) % (lockTick + SHOWN_CLEARED))

    /** This preview at [level]: the same if it is already at it, a new T coming in otherwise. */
    fun withLevel(level: Level): PreviewReplay = if (level == this.level) this else PreviewReplay(level)

    /** The T in play, one row lower every `ticksPerRow` until it lands; null once it has locked. */
    val piece: Piece?
        get() = if (locked) null else SPAWNED.droppedBy(minOf(elapsed / ticksPerRow, LANDING_DROPS))

    /** The stack with its two holes, then, once the T has locked, without the two lines it filled. */
    val board: Board get() = if (locked) CLEARED.board else START_BOARD

    /** No points, then those of the lines cleared at [level]. */
    val points: Points
        get() = if (locked) ClassicScoringRule.pointsForLinesCleared(CLEARED.lineClear, level) else Points(0)

    companion object {
        /** The rows of the board the preview shows, the bottom ones. */
        const val PREVIEW_ROWS: Int = 6

        /** The preview standing still, in reduced effects: what the replay ends on, its lines cleared, its points. */
        fun still(level: Level): PreviewReplay = PreviewReplay(level, elapsed = lockTick(level))

        private fun ticksPerRow(level: Level): Int = ClassicGravityTable.ticksPerRow(level).value

        private fun lockTick(level: Level): Int = (LANDING_DROPS + 1) * ticksPerRow(level)

        /** How long the lines show cleared, with their points, before the T comes in again: 2 seconds. */
        private const val SHOWN_CLEARED = 120

        /** A stack whose two bottom lines miss the cells of a T pointing down. */
        private val START_BOARD: Board = Board.parse(
            """
            .Z......J.
            ZZZI...JJL
            SSOOI.LLLL
            """,
        )

        /** The T as it comes in, pointing down, its top row the first of the rows shown. */
        private val SPAWNED = Piece(
            PieceType.T,
            ClassicRotationSystem.spawnOrientation(PieceType.T),
            Position(column = 5, row = START_BOARD.rows - PREVIEW_ROWS),
        )

        private val LANDING_DROPS: Int = START_BOARD.landing(SPAWNED).pivot.row - SPAWNED.pivot.row

        private val CLEARED = START_BOARD.lock(START_BOARD.landing(SPAWNED)).clearFullRows()

        private fun Piece.droppedBy(rows: Int): Piece = copy(pivot = pivot.copy(row = pivot.row + rows))
    }
}
