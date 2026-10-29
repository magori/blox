package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.component.sheetExitTicks
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.fadeOut
import ch.magori.blox.core.designsystem.theme.swell
import ch.magori.blox.core.game.model.Position

/**
 * The feedback of the change of the garden numbered [move] (two same changes in a row are two feedbacks): the
 * [change], on the cells of the garden it concerns, each with the [rank] of its piece in the fall of Au hasard (0 for
 * the other changes). In ticks (ADR 0006, 0024): the screen counts them from the change, and reads [light], [lift] and
 * [shown] in its drawing alone. Immutable for Compose: its [rank]s are never changed once built.
 */
@Immutable
internal data class GardenFeedback(val change: GardenChange, val rank: Map<Position, Int>, val move: Int) {
    /** Whether its pieces fall one by one: after Au hasard, or a kept garden opened once the sheet has gone down. */
    private val falls: Boolean = change == GardenChange.Rolled || change == GardenChange.Opened

    /** The tick the first piece starts to fall at: a kept garden opened waits for the sheet to go down. */
    private val fallDelay: Int = if (change == GardenChange.Opened) sheetExitTicks(BloxEffects.Full) else 0

    /** How many ticks this feedback lasts in [effects]. */
    fun ticks(effects: BloxEffects): Int = when {
        change == GardenChange.Opened && effects.reduced -> 0
        falls && effects.reduced -> BloxMotion.reducedFade
        falls -> fallDelay + BloxMotion.gardenFallStep * (rank.values.maxOrNull() ?: 0) + FALL_TICKS
        else -> BloxMotion.gardenGlow
    }

    /** How bright the note on [cell] is lit at [tick] in [effects], from 0 (not at all) to 1. */
    fun light(
        cell: Position,
        tick: Int,
        effects: BloxEffects,
    ): Float = if (!falls && cell in rank) fadeOut(tick, ticks(effects)) else 0f

    /**
     * How far above its place the note on [cell] is drawn at [tick] in [effects], in cells: a piece of Au hasard falls
     * there, never in reduced effects.
     */
    fun lift(
        cell: Position,
        tick: Int,
        effects: BloxEffects,
    ): Float {
        if (effects.reduced) return 0f
        val falling = tick - (fallStart(cell) ?: return 0f)
        if (falling >= FALL_TICKS) return 0f
        // As it falls, faster and faster: the height left goes down with the square of the time.
        val progress = falling.coerceAtLeast(0).toFloat() / FALL_TICKS
        return FALL_CELLS * (1f - progress * progress)
    }

    /**
     * How much of the note on [cell] is shown at [tick] in [effects], from 0 (the free cell) to 1 (the note): a piece of
     * Au hasard is out of sight until it falls; in reduced effects, they all fade in together, but those of a kept garden
     * opened, shown at once.
     */
    fun shown(
        cell: Position,
        tick: Int,
        effects: BloxEffects,
    ): Float {
        val start = fallStart(cell) ?: return 1f
        return when {
            effects.reduced -> 1f - fadeOut(tick, ticks(effects))
            tick < start -> 0f
            else -> 1f
        }
    }

    /** The tick the piece on [cell] starts to fall at; null when it does not fall. */
    private fun fallStart(cell: Position): Int? =
        rank[cell]?.takeIf { falls }?.let { fallDelay + it * BloxMotion.gardenFallStep }
}

/** The feedback of the last change of [state]; none when the screen animates none. */
internal fun gardenFeedbackOf(state: GardenUiState): GardenFeedback? = when (val change = state.change) {
    is GardenChange.Placed -> GardenFeedback(change, change.piece.cells.associateWith { 0 }, state.move)

    GardenChange.Rolled, GardenChange.Opened -> {
        val pieces = state.garden.pieces.sortedBy { piece -> piece.cells.minOf { it.column } }
        val rank = pieces.withIndex().flatMap { (rank, piece) -> piece.cells.map { it to rank } }.toMap()
        GardenFeedback(change, rank, state.move)
    }

    GardenChange.MoodChosen ->
        GardenFeedback(change, state.garden.pieces.flatMap { it.cells }.associateWith { 0 }, state.move)

    null -> null
}

/** How long a piece of Au hasard takes to fall to its place. */
private const val FALL_TICKS = 8

/** How high above its place a piece of Au hasard starts to fall, in cells. */
private const val FALL_CELLS = 1f

/**
 * How large the notes of the column played are drawn, [tick]s after the play head came onto it, in [effects]: they
 * bounce up to [BOUNCE] times their size and back, within `motion-garden-bounce`; never in reduced effects, the play
 * head going from column to column.
 */
internal fun playHeadBounce(
    tick: Int,
    effects: BloxEffects,
): Float = if (effects.reduced || tick < 0 || tick >= BloxMotion.gardenBounce) {
    // At rest, the bounce over: a tick long gone, as Int.MAX_VALUE, never reaches the tick ahead below.
    1f
} else {
    // A tick ahead, so that the notes are larger from the first frame of the eighth.
    1f + (BOUNCE - 1f) * swell(tick + 1, BloxMotion.gardenBounce)
}

/** How large the notes of the column played grow at most. */
private const val BOUNCE = 1.1f
