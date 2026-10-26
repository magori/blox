package ch.magori.blox.feature.locksmith

import androidx.annotation.StringRes
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.Refusal

/**
 * The message under the padlock of [state]: the padlock open, above all; else why the last placement was refused; else
 * what the hint shown points at; else how to play, with a preview or without.
 */
@StringRes
internal fun messageOf(state: LocksmithUiState): Int {
    val refusal = state.events.filterIsInstance<PadlockEvent.PlacementRefused>().firstOrNull()?.reason
    return when {
        state.game.isOpen -> R.string.feature_locksmith_message_open
        refusal == Refusal.OUT_OF_HOLE -> R.string.feature_locksmith_message_out_of_hole
        refusal == Refusal.OVERLAP -> R.string.feature_locksmith_message_overlap
        state.hint is Hint.Fit -> R.string.feature_locksmith_message_hint
        state.hint is Hint.Blocking -> R.string.feature_locksmith_message_blocking
        state.aim != null -> R.string.feature_locksmith_message_preview
        else -> R.string.feature_locksmith_message_aim
    }
}
