package ch.magori.blox.feature.game.gameover

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.feedback.SCORE_COUNT_TICKS
import ch.magori.blox.feature.game.feedback.countingUp
import kotlin.math.PI
import kotlin.math.sin

/** The game over as it plays, counted in ticks of 1/60 s, over the [rows] of the board. */
@Immutable
internal data class GameOverPlay(
    val tick: Int,
    val reduced: Boolean,
    val newBest: Boolean,
    val rows: Int = BOARD_ROWS,
) {
    /** Whether [row] of the board, counted from its top, has gone out. */
    fun rowOut(row: Int): Boolean = row >= rows - rowsOut

    /** How many rows have gone out, from the bottom. */
    private val rowsOut: Int get() = if (reduced) rows else minOf(rows, tick / TICKS_PER_ROW)

    /** Whether the card shows. */
    val cardShown: Boolean get() = rowsOut == rows

    /** The final [score] as the card shows it. */
    fun shownScore(score: Points): Points {
        if (reduced) return score
        return countingUp(from = Points(0), to = score, elapsed = maxOf(0, tick - cardTick))
    }

    /** The tick the card shows on. */
    private val cardTick: Int get() = if (reduced) 0 else rows * TICKS_PER_ROW

    /** Whether it still plays: it needs frames. */
    val playing: Boolean get() = tick < end

    /** The tick it ends on: once the score has counted up, and the confetti, if any, have fallen. */
    private val end: Int
        get() = if (reduced) 0 else cardTick + maxOf(SCORE_COUNT_TICKS, if (newBest) CONFETTI_TICKS else 0)

    /** This game over played to its end. */
    fun ended(): GameOverPlay = copy(tick = maxOf(tick, end))

    /** How many confetti it throws. */
    val confettiCount: Int get() = if (newBest && !reduced) CONFETTI else 0

    /** How the confetto [index] looks, if it shows. */
    fun confetto(index: Int): ConfettoLook? {
        val elapsed = tick - cardTick
        val life = CONFETTI_TICKS * (1f - SPREAD * fraction(index * LIFE_STEP))
        val progress = elapsed / life
        if (index >= confettiCount || elapsed < 0 || progress >= 1f) return null
        return ConfettoLook(
            x = fraction(START + index * GOLDEN) + SWAY * sin(2 * PI.toFloat() * (progress * 2 + index * LIFE_STEP)),
            y = -TOP_ROOM + (1f + TOP_ROOM) * (progress + progress * progress) / 2,
            angle = (index * TURN_STEP) % FULL_TURN + elapsed * (MIN_SPIN + index % SPINS),
            alpha = minOf(1f, (1f - progress) / FADE),
            type = PieceType.entries[index % PieceType.entries.size],
        )
    }

    /** This game over [ticks] later. */
    fun advance(ticks: Int): GameOverPlay = copy(tick = tick + ticks)

    companion object {
        /** The rows of a board of the game. */
        private val BOARD_ROWS = Board.empty().rows

        /** The ticks each row takes to go out (docs/design-system/README.md, Retours de jeu). */
        private const val TICKS_PER_ROW = 2

        /** The confetti of a new best: 40 particles at most (docs/design-system/README.md, Retours de jeu). */
        private const val CONFETTI = 40

        /** How long the slowest confetto falls. */
        private const val CONFETTI_TICKS = 90

        /** How much shorter the fall of a quicker one is, at most. */
        private const val SPREAD = 0.3f
        private const val LIFE_STEP = 0.37f
        private const val START = 0.07f
        private const val GOLDEN = 0.618034f
        private const val SWAY = 0.03f
        private const val TOP_ROOM = 0.05f
        private const val TURN_STEP = 47f
        private const val FULL_TURN = 360f
        private const val MIN_SPIN = 6
        private const val SPINS = 4

        /** The part of its fall during which a confetto fades out, at its end. */
        private const val FADE = 0.2f

        /**
         * A game over that starts to play, with [effects], for a game that made a [newBest] or not, over the [rows] of
         * its board.
         */
        fun starting(
            effects: BloxEffects,
            newBest: Boolean,
            rows: Int = BOARD_ROWS,
        ): GameOverPlay = GameOverPlay(tick = 0, reduced = effects.reduced, newBest = newBest, rows = rows)

        /** The part of [value] after its point, from 0 to 1. */
        private fun fraction(value: Float): Float = value - value.toInt()
    }
}

/**
 * How a confetto looks on one tick: at [x] and [y] of the room it falls in, from 0 to 1 from its top-left corner, turned
 * by [angle] degrees, at [alpha], in the colour of a piece of [type].
 */
@Immutable
internal data class ConfettoLook(val x: Float, val y: Float, val angle: Float, val alpha: Float, val type: PieceType)
