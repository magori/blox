package ch.magori.blox.feature.locksmith

import androidx.annotation.StringRes
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.Refusal

/**
 * The message under the padlock of [state]: the padlock open, above all; else the progress just cleared; else why the
 * last placement was refused; else the orientation of the piece just turned; else what the hint shown points at; else
 * how to play, with a preview or without.
 */
@StringRes
internal fun messageOf(state: LocksmithUiState): Int {
    val refusal = state.events.filterIsInstance<PadlockEvent.PlacementRefused>().firstOrNull()?.reason
    return when {
        state.game.isOpen -> R.string.feature_locksmith_message_cadence
        state.cleared -> R.string.feature_locksmith_message_cleared
        refusal == Refusal.OUT_OF_HOLE -> R.string.feature_locksmith_message_out_of_hole
        refusal == Refusal.OVERLAP -> R.string.feature_locksmith_message_overlap
        state.turned -> R.string.feature_locksmith_message_turned
        state.hint is Hint.Fit -> R.string.feature_locksmith_message_hint
        state.hint is Hint.Blocking -> R.string.feature_locksmith_message_blocking
        state.aim != null -> R.string.feature_locksmith_message_preview
        else -> R.string.feature_locksmith_message_aim
    }
}

/**
 * What TalkBack reads of the title of the bar of [state]: its number, closed, then open once the padlock is; the padlock
 * drawn beside it is left out of the tree, so that the opening is told once, by the card.
 */
@StringRes
internal fun barTitleOf(state: LocksmithUiState): Int =
    if (state.game.isOpen) R.string.feature_locksmith_title_open else R.string.feature_locksmith_title_closed

/**
 * The number of the refusal the last action of [state] told, among the refusals of the game; null when it told none.
 * TalkBack reads it with the reason, so that a second refusal in a row, of the same reason, is told again.
 */
internal fun refusalNumberOf(state: LocksmithUiState): Int? =
    if (state.events.any { it is PadlockEvent.PlacementRefused }) state.game.refusals else null

/** The given piece the hint of [state] names: the one that fits where it marks, or the placed one that blocks. */
internal fun hintedPieceOf(state: LocksmithUiState): PieceType? = when (val hint = state.hint) {
    is Hint.Fit -> hint.piece.type
    is Hint.Blocking -> state.game.given[hint.rank]
    null -> null
}
