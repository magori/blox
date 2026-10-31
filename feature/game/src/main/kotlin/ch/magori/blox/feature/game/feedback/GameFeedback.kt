package ch.magori.blox.feature.game.feedback

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.feature.game.controls.ControlButton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
    val levelUp: Int? = null,
    val danger: Int? = null,
    val bestBeaten: Int? = null,
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
        val cleared = events.filterIsInstance<GameEvent.LinesCleared>().lastOrNull()
        val ended = clear?.takeIf { cleared != null }
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
            points = ended?.let {
                PointsGained(scoreBefore, score, it.middle, tick, fourLines = cleared?.lineClear?.isFourLines == true)
            } ?: points,
            levelUp = if (events.any { it is GameEvent.LevelUp }) tick else levelUp,
            danger = when (events.filterIsInstance<GameEvent.StackDanger>().lastOrNull()?.on) {
                true -> danger ?: tick
                false -> null
                null -> danger
            },
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
     * Four lines cleared at once make it the sticker « 4 LIGNES », with their points.
     */
    fun sticker(reduced: Boolean): StickerLook? {
        val gained = points ?: return null
        val elapsed = tick - gained.tick
        if (elapsed >= BloxMotion.pop) return null
        val look = StickerLook(gained.points, gained.row, rise = 0f, alpha = 1f, fourLines = gained.fourLines)
        if (reduced) return look.copy(alpha = reducedAlpha(elapsed))
        val progress = elapsed.toFloat() / BloxMotion.pop
        val alpha = if (progress < STICKER_FADE_FROM) 1f else (1f - progress) / (1f - STICKER_FADE_FROM)
        return look.copy(rise = STICKER_RISE * easeOut(progress), alpha = alpha)
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

    /**
     * The iridescent border of four lines cleared at once, while it shows: a reflection that goes once around the frame
     * of the board in [BloxMotion.pop] ticks, from the tick the rows are cleared; null otherwise.
     */
    fun irisBorder(reduced: Boolean): IrisBorder? {
        val gained = points?.takeIf { it.fourLines } ?: return null
        val elapsed = tick - gained.tick
        if (elapsed >= BloxMotion.pop) return null
        if (reduced) return IrisBorder(head = 0f, whole = true, alpha = reducedAlpha(elapsed))
        return IrisBorder(head = elapsed.toFloat() / BloxMotion.pop)
    }

    /** This feedback once the score beat the best, on this tick: the first time only, the best stays beaten. */
    fun withBestBeaten(): GameFeedback = if (bestBeaten != null) this else copy(bestBeaten = tick)

    /**
     * How large the sticker of a new best is drawn, 1 as it is: once the best is beaten, it swells, shrinks a little, and
     * comes back to its size in [BloxMotion.sticker] ticks. In reduced effects, it takes the place of the best at its size.
     */
    fun bestBounce(reduced: Boolean): Float {
        if (reduced) return 1f
        val elapsed = tick - (bestBeaten ?: return 1f)
        return 1f + BEST_BOUNCE * wobble(elapsed.toFloat() / BloxMotion.sticker)
    }

    /** Whether the stack is in danger: from the tick it entered it, until it leaves it. */
    val inDanger: Boolean get() = danger != null

    /**
     * How much `danger-soft` covers [row], from 0 to 1: while the stack is in danger, the [DANGER_ROWS] top rows breathe,
     * whole as it enters the danger, down to [DANGER_LOW] and back in [DANGER_BREATH_TICKS]. In reduced effects, whole
     * and still.
     */
    fun dangerTint(
        row: Int,
        reduced: Boolean,
    ): Float {
        val entered = danger ?: return 0f
        if (row >= DANGER_ROWS) return 0f
        if (reduced) return 1f
        val breath = (1f + cos(2 * PI.toFloat() * (tick - entered) / DANGER_BREATH_TICKS)) / 2
        return DANGER_LOW + (1f - DANGER_LOW) * breath
    }

    /**
     * How the panel of the level looks on this tick: once a level is gained, framed for [LEVEL_FRAME_TICKS], and tilted
     * one way then the other during [LEVEL_TILT_TICKS], back straight. In reduced effects, framed alike, never tilted.
     */
    fun levelLook(reduced: Boolean): LevelLook {
        val elapsed = tick - (levelUp ?: return LevelLook.Rest)
        val tilt = if (reduced) 0f else LEVEL_TILT * wobble(elapsed.toFloat() / LEVEL_TILT_TICKS)
        return LevelLook(framed = elapsed < LEVEL_FRAME_TICKS, tilt = tilt)
    }

    private companion object {
        /** How much larger the sticker of a new best swells as it bounces. */
        const val BEST_BOUNCE = 0.15f

        /** How long the rows in danger take to breathe out and in again: once in 2 s, far from a flash. */
        const val DANGER_BREATH_TICKS = 120

        /** How little of `danger-soft` covers them at the bottom of their breath. */
        const val DANGER_LOW = 0.5f

        /** How long the level stays framed once gained: 1 s (docs/design-system/README.md, Retours de jeu). */
        const val LEVEL_FRAME_TICKS = 60

        /** How long the level tilts once gained, one way then the other. */
        const val LEVEL_TILT_TICKS = 30

        /** How far the level tilts at most, in degrees. */
        const val LEVEL_TILT = 8f

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

/**
 * A swing at [progress] of it, from 0 to 1: from 0 up to nearly 1, down below 0, and back to 0, less and less; 0 once
 * over.
 */
private fun wobble(progress: Float): Float =
    if (progress >= 1f) 0f else sin(2 * PI.toFloat() * progress) * (1f - progress)

/** The rows at the top of the board tinted while the stack is in danger. */
internal const val DANGER_ROWS = 5

/**
 * The opacity, in reduced effects, of what shows still for [BloxMotion.pop] ticks, [elapsed] ticks after it showed: whole,
 * then fading during its last [BloxMotion.reducedFade] ticks.
 */
private fun reducedAlpha(elapsed: Int): Float = minOf(1f, (BloxMotion.pop - elapsed) / (BloxMotion.reducedFade + 1f))

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
 * from its top; [fourLines] when it cleared four lines at once.
 */
@Immutable
internal data class PointsGained(
    val from: Points,
    val to: Points,
    val row: Float,
    val tick: Int,
    val fourLines: Boolean = false,
) {
    val points: Int get() = to.value - from.value
}

/**
 * How the panel of the level looks on one tick: [framed] in `accent`, and turned by [tilt] degrees, clockwise, around
 * its centre.
 */
@Immutable
internal data class LevelLook(val framed: Boolean, val tilt: Float) {
    companion object {
        /** As at rest: neither framed nor tilted. */
        val Rest = LevelLook(framed = false, tilt = 0f)
    }
}

/**
 * How the iridescent border of four lines looks on one tick: a reflection whose start is at [head] of the way around
 * the frame, from 0 to 1, or the [whole] frame iridescent (reduced effects), at [alpha].
 */
@Immutable
internal data class IrisBorder(val head: Float, val whole: Boolean = false, val alpha: Float = 1f)

/**
 * How the sticker of the [points] gained looks on one tick: centred on [row] of the board, in cells from its top, but
 * [rise] cells higher, at [alpha]; the sticker « 4 LIGNES » over the points when they came from [fourLines] at once.
 */
@Immutable
internal data class StickerLook(
    val points: Int,
    val row: Float,
    val rise: Float,
    val alpha: Float,
    val fourLines: Boolean = false,
)
