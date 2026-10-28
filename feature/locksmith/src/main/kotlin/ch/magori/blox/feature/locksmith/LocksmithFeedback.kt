package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.fadeOut
import ch.magori.blox.core.designsystem.theme.swell
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.PadlockEvent
import kotlin.math.PI
import kotlin.math.sin

/** What the last action of the player shows on the padlock. */
internal enum class FeedbackKind { PLACED, REFUSED, OPENED, HINTED }

/**
 * The feedback of the action numbered [move] (two refusals in a row are two feedbacks): its [kind], on the [cells] of
 * the padlock it concerns. In ticks (ADR 0006, 0024): the screen counts them from the action, and reads [glow] and
 * [shake] in its drawing alone. Immutable for Compose: its [cells] are never changed once built.
 */
@Immutable
internal data class Feedback(val kind: FeedbackKind, val cells: Set<Position>, val move: Int) {
    /** The lowest row of the [cells], where the wave of an open padlock starts. */
    private val bottom: Int = cells.maxOfOrNull { it.row } ?: 0

    /** How many rows the wave of an open padlock climbs above the [bottom]. */
    private val rise: Int = bottom - (cells.minOfOrNull { it.row } ?: 0)

    /** How many ticks this feedback lasts in [effects]; none, it shows nothing. */
    fun ticks(effects: BloxEffects): Int = when (kind) {
        FeedbackKind.PLACED -> if (effects.reduced) 0 else LIGHT_TICKS
        FeedbackKind.REFUSED -> if (effects.reduced) BloxMotion.reducedFade else SHAKE_TICKS
        FeedbackKind.OPENED -> if (effects.reduced) BloxMotion.reducedFade else WAVE_STEP * rise + LIGHT_TICKS
        FeedbackKind.HINTED -> if (effects.reduced) BloxMotion.reducedFade else BloxMotion.sticker
    }

    /**
     * How bright [cell] is lit at [tick] in [effects], from 0 (not at all) to 1: a placement lights its piece; an open
     * padlock its hole, row by row from the bottom up, or all at once in reduced effects; a refusal, in reduced effects
     * only, its preview, the fade that takes the place of the shake.
     */
    fun glow(
        cell: Position,
        tick: Int,
        effects: BloxEffects,
    ): Float = when {
        cell !in cells -> 0f
        kind == FeedbackKind.REFUSED && !effects.reduced -> 0f
        kind == FeedbackKind.OPENED && !effects.reduced -> fadeOut(tick - wave(cell), LIGHT_TICKS)
        kind == FeedbackKind.HINTED && !effects.reduced -> swell(tick, ticks(effects))
        else -> fadeOut(tick, ticks(effects))
    }

    /** How many ticks after the bottom row the wave of an open padlock reaches the row of [cell]. */
    private fun wave(cell: Position): Int = WAVE_STEP * (bottom - cell.row)

    /**
     * How far the padlock is shaken aside at [tick] in [effects], in parts of its amplitude, from -1 (to the left) to 1:
     * after a refusal, back and forth every [SHAKE_PERIOD] ticks, less and less, back in place at the end; never in
     * reduced effects.
     */
    fun shake(
        tick: Int,
        effects: BloxEffects,
    ): Float {
        val length = ticks(effects)
        if (kind != FeedbackKind.REFUSED || effects.reduced || tick >= length) return 0f
        // A tick ahead, so that the padlock is aside from the first frame of the refusal.
        return sin(2 * PI * (tick + 1) / SHAKE_PERIOD).toFloat() * fadeOut(tick, length)
    }
}

/** The feedback of the last action of [state]; none when it shows nothing. */
internal fun feedbackOf(state: LocksmithUiState): Feedback? {
    val events = state.events
    return when {
        PadlockEvent.PadlockOpened in events -> Feedback(FeedbackKind.OPENED, state.game.padlock.hole, state.move)

        events.any { it is PadlockEvent.HintGiven } -> Feedback(FeedbackKind.HINTED, hintedCellsOf(state), state.move)

        events.any { it is PadlockEvent.PlacementRefused } ->
            Feedback(FeedbackKind.REFUSED, crossedCellsOf(state), state.move)

        else -> events.filterIsInstance<PadlockEvent.PiecePlaced>().firstOrNull()?.let { placed ->
            Feedback(FeedbackKind.PLACED, placed.piece.cells, state.move)
        }
    }
}

/** The cells the hint of [state] marks: where its piece fits, or the placed piece that blocks. */
private fun hintedCellsOf(state: LocksmithUiState): Set<Position> = when (val hint = state.hint) {
    is Hint.Fit -> hint.piece.cells
    is Hint.Blocking -> state.game.placed[hint.rank]?.cells.orEmpty()
    null -> emptySet()
}

/** A cell lit stays as long as a lock of the Classic, `PieceLocked`. */
private const val LIGHT_TICKS = 8

/** An open padlock lights its hole row by row, from the bottom up, a row every 2 ticks. */
private const val WAVE_STEP = 2

/** A shake goes back and forth in 6 ticks, 10 times a second: a move, not a flash. */
private const val SHAKE_PERIOD = 6

/** A refusal shakes the padlock three times (`motion-card`, 300 ms). */
private const val SHAKE_TICKS = 18

/**
 * The cells of the preview of [state] that the screen crosses out: off the hole, those on the ring included, or over a
 * placed piece.
 */
private fun crossedCellsOf(state: LocksmithUiState): Set<Position> =
    state.preview?.cells.orEmpty().filterNot(state.game::holds).toSet()
