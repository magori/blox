package ch.magori.blox.feature.game.feedback

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.feature.game.controls.ControlButton
import kotlin.math.abs

/**
 * The feedback of the game in progress (ADR 0024): what happened, and on which tick of the game, counted as the
 * engine counts them ([tick], its `tickCount`). The screen reads, for the tick, how each effect looks; a game paused
 * plays no tick, and so freezes its effects. No colour here, no time but ticks.
 */
@Immutable
internal data class GameFeedback(
    val tick: Int = 0,
    val lock: LockedCells? = null,
    val turns: Map<ControlButton, Int> = emptyMap(),
    val clear: RowsClear? = null,
    val slide: RowsSlide? = null,
    val points: PointsGained? = null,
) {
    /**
     * This feedback once the game played the tick [tick], which sent [events]; [clearing] is the line clear the game is
     * in after it, if any, and the score went from [scoreBefore] to [score].
     */
    fun after(
        tick: Int,
        events: List<GameEvent>,
        clearing: ClearingRows?,
        scoreBefore: Points,
        score: Points,
    ): GameFeedback {
        val locked = events.filterIsInstance<GameEvent.PieceLocked>().lastOrNull()
        val turned = events.filterIsInstance<GameEvent.PieceRotated>().lastOrNull()
        // The clear that ends on this tick, if any: its rows are gone from the board.
        val ended = clear?.takeIf { events.any { it is GameEvent.LinesCleared } }
        return copy(
            tick = tick,
            lock = locked?.let { LockedCells(it.piece.cells, tick) } ?: lock,
            turns = turned?.let { turns + (it.rotation.key to tick) } ?: turns,
            // The clear starts with the lock that fills the rows: its length is what is left of it then.
            clear = when {
                clearing == null -> null
                locked != null -> RowsClear(clearing.rows, tick, duration = clearing.ticksLeft)
                else -> clear
            },
            slide = ended?.let { RowsSlide(it.rows, tick) } ?: slide,
            points = ended?.let { PointsGained(scoreBefore, score, it.middle, tick) } ?: points,
        )
    }

    /**
     * How much the cell at [column] and [row] is lit, from 0 to 1: the cells of a piece that locked, for [LOCK_TICKS].
     */
    fun lockLight(
        column: Int,
        row: Int,
        reduced: Boolean,
    ): Float {
        val locked = lock?.takeUnless { reduced } ?: return 0f
        val elapsed = tick - locked.tick
        if (elapsed >= LOCK_TICKS || locked.cells.none { it.column == column && it.row == row }) return 0f
        return LOCK_LIGHT * (LOCK_TICKS - elapsed) / LOCK_TICKS
    }

    /**
     * The angle the icon of the key [button] is drawn at, in degrees, clockwise: once its rotation turned the piece, a
     * quarter turn behind, back to its place in [TURN_TICKS]; 0 otherwise.
     */
    fun keyAngle(
        button: ControlButton,
        reduced: Boolean,
    ): Float {
        if (reduced) return 0f
        val elapsed = tick - (turns[button] ?: return 0f)
        if (elapsed >= TURN_TICKS) return 0f
        val behind = QUARTER_TURN * (TURN_TICKS - elapsed) / TURN_TICKS
        return if (button == ControlButton.A) -behind else behind
    }

    /** Whether [row] glows (`clear-glow`): a full row, while it is cleared, in full and in reduced effects. */
    fun rowGlows(row: Int): Boolean = clear?.rows?.contains(row) == true

    /**
     * How large the cell at [column] and [row] is drawn, 1 as it is, 0 not at all: the cells of the full rows pop by pairs, from
     * the centre to the edges, one pair after the other, the last one gone on the last tick of the clear. Each one swells
     * a little, then shrinks to nothing.
     */
    fun cellScale(
        column: Int,
        row: Int,
        reduced: Boolean,
    ): Float {
        val cleared = clear ?: return 1f
        if (row !in cleared.rows) return 1f
        // In reduced effects, the full rows glow at once, without their cells.
        if (reduced) return 0f
        // The pairs pop during the ticks of the clear after the one of the lock, a fifth of them each.
        val pair = abs(2 * column - (ClassicBoard.columns - 1)) / 2
        val span = (cleared.duration - 1).coerceAtLeast(1).toFloat()
        val progress = ((tick - cleared.tick) * (ClassicBoard.columns / 2) / span - pair).coerceIn(0f, 1f)
        return pop(progress)
    }

    /**
     * How far above its place [row] of the board is drawn, in cells: once full rows are cleared, each row is first drawn
     * where it stood, as many cells higher as rows were cleared under it, then comes down to its place in [SLIDE_TICKS],
     * slower and slower.
     */
    fun rowLift(
        row: Int,
        reduced: Boolean,
    ): Float {
        val slid = slide?.takeUnless { reduced } ?: return 0f
        val elapsed = tick - slid.tick
        if (elapsed >= SLIDE_TICKS) return 0f
        return slid.fall(row) * (1f - easeOut(elapsed.toFloat() / SLIDE_TICKS))
    }

    /**
     * The sticker « +points » of the last line clear, while it shows: in the middle of the cleared rows, it rises by
     * [STICKER_RISE] cells, slower and slower, and fades in the second half of its [BloxMotion.pop] ticks; null once
     * gone. In reduced effects, it stays where it shows, and fades only during its last [BloxMotion.reducedFade] ticks.
     */
    fun sticker(reduced: Boolean): StickerLook? {
        val gained = points ?: return null
        val elapsed = tick - gained.tick
        if (elapsed >= BloxMotion.pop) return null
        if (reduced) {
            val left = BloxMotion.pop - elapsed
            val alpha = minOf(1f, left / (BloxMotion.reducedFade + 1f))
            return StickerLook(gained.points, gained.row, rise = 0f, alpha = alpha)
        }
        val progress = elapsed.toFloat() / BloxMotion.pop
        val alpha = if (progress < STICKER_FADE_FROM) 1f else (1f - progress) / (1f - STICKER_FADE_FROM)
        return StickerLook(gained.points, gained.row, rise = STICKER_RISE * easeOut(progress), alpha = alpha)
    }

    /**
     * The score shown for the [score] of the game: once rows are cleared, it counts up from the score before to its new
     * value in [COUNT_TICKS]; [score] as it is otherwise, as when it changed again since.
     */
    fun shownScore(
        score: Points,
        reduced: Boolean,
    ): Points {
        val gained = points?.takeUnless { reduced } ?: return score
        val elapsed = tick - gained.tick
        if (score != gained.to || elapsed >= COUNT_TICKS) return score
        return Points(gained.from.value + gained.points * elapsed / COUNT_TICKS)
    }

    private companion object {
        /** How long the score takes to count up to its new value. */
        const val COUNT_TICKS = 30

        /** How many cells the sticker of the points rises. */
        const val STICKER_RISE = 1.5f

        /** The part of its time after which the sticker of the points fades. */
        const val STICKER_FADE_FROM = 0.5f

        /** How long the rows above cleared rows take to come down to their place. */
        const val SLIDE_TICKS = 8

        /** How long the cells of a piece that locked stay lit (docs/design-system/README.md, Retours de jeu). */
        const val LOCK_TICKS = 8

        /** How much they are lit on the tick of the lock, then less and less. */
        const val LOCK_LIGHT = 0.6f

        /** How long the icon of a key of rotation takes to turn (docs/design-system/README.md, Retours de jeu). */
        const val TURN_TICKS = 6

        const val QUARTER_TURN = 90f
    }
}

/** How much larger a cell that pops swells, before it shrinks. */
private const val POP_SWELL = 0.2f

/** The part of its pop during which a cell swells; it shrinks during the rest. */
private const val POP_SWELLING = 1f / 3

/** The scale of a cell at [progress] of its pop, from 0 to 1: it swells, then shrinks to nothing. */
private fun pop(progress: Float): Float = if (progress < POP_SWELLING) {
    1f + POP_SWELL * progress / POP_SWELLING
} else {
    (1f + POP_SWELL) * (1f - progress) / (1f - POP_SWELLING)
}

/** The key that turns a piece by [this] rotation: A clockwise, B counterclockwise. */
private val Rotation.key: ControlButton
    get() = when (this) {
        Rotation.CLOCKWISE -> ControlButton.A
        Rotation.COUNTERCLOCKWISE -> ControlButton.B
    }

/** The [cells] of a piece that locked on [tick]. */
@Immutable
internal data class LockedCells(val cells: Set<Position>, val tick: Int)

/** The full [rows] of a line clear, cleared [ticksLeft] ticks from now (`Phase.Clearing`). */
@Immutable
internal data class ClearingRows(val rows: List<Int>, val ticksLeft: Int)

/** The full [rows] of a line clear that started on [tick] and lasts [duration] ticks. */
@Immutable
internal data class RowsClear(val rows: List<Int>, val tick: Int, val duration: Int) {
    /** The middle of the rows, in cells from the top of the board. */
    val middle: Float get() = (rows.min() + rows.max() + 1) / 2f
}

/** The full [rows] cleared on [tick], from top to bottom: the rows above them come down. */
@Immutable
internal data class RowsSlide(val rows: List<Int>, val tick: Int) {
    /**
     * How many rows [row] of the cleared board fell: as many as were cleared under the row it was. The new empty rows at
     * the top fell as many as were cleared.
     */
    fun fall(row: Int): Int = falls[row]

    // Computed once, not for each row of each frame.
    private val falls: IntArray = ClassicBoard.rows.let { boardRows ->
        val kept = (0 until boardRows).filter { it !in rows }
        IntArray(boardRows) { row -> if (row < rows.size) rows.size else row - kept[row - rows.size] }
    }
}

/** The board of the game, 10 × 20 cells: its columns pop by pairs, its rows slide, the sticker sits on it. */
internal val ClassicBoard: Board = Board.empty()

/** [progress], from 0 to 1, slower and slower towards its end. */
private fun easeOut(progress: Float): Float = 1f - (1f - progress) * (1f - progress)

/**
 * The points a line clear gained on [tick], the score going from [from] to [to], shown at [row] of the board, in cells
 * from its top.
 */
@Immutable
internal data class PointsGained(val from: Points, val to: Points, val row: Float, val tick: Int) {
    val points: Int get() = to.value - from.value
}

/**
 * How the sticker of the [points] gained looks on one tick: centred on [row] of the board, in cells from its top, but
 * [rise] cells higher, at [alpha].
 */
@Immutable
internal data class StickerLook(val points: Int, val row: Float, val rise: Float, val alpha: Float)
