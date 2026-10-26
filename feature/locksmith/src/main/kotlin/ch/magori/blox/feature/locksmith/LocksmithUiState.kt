package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame

/**
 * What the screen of the Locksmith shows: padlock [level], from 1, and its [game]; the [events] of the last action of
 * the player, for the feedback and the sounds, and [move], counting every action, so that two same events in a row
 * stay two feedbacks; the [hint] shown, until the next placement accepted, a piece taken back or a restart.
 */
internal data class LocksmithUiState(
    val level: Int,
    val game: PadlockGame,
    val events: List<PadlockEvent> = emptyList(),
    val move: Int = 0,
    val hint: Hint? = null,
)
