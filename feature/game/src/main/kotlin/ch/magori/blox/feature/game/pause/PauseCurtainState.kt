package ch.magori.blox.feature.game.pause

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * The curtain of the pause of a game screen, [curtain], none while the game goes on. A game shown already [paused]
 * has its curtain shut at once; one that pauses later brings it down, with the [BloxEffects] of that moment.
 */
@Stable
internal class PauseCurtainState(paused: Boolean, effects: BloxEffects) {
    var curtain: PauseCurtain? by mutableStateOf(if (paused) PauseCurtain.closed(effects) else null)
        private set

    /** Whether the curtain is open again after Reprendre: the game may go on. */
    val gameMayGoOn: Boolean get() = curtain?.phase == CurtainPhase.OPEN

    /** The game is [paused] or not: a pause brings the curtain down, with [effects]; a game that goes on has none. */
    fun follow(
        paused: Boolean,
        effects: BloxEffects,
    ) {
        curtain = if (paused) curtain ?: PauseCurtain.closing(effects) else null
    }

    /** Reprendre: the curtain opens from where it stands, at once if it had not come yet. */
    fun resume() {
        curtain = (curtain ?: PauseCurtain.closing(BloxEffects.Full)).opening()
    }

    /** The curtain [ticks] later. */
    fun advance(ticks: Int) {
        curtain = curtain?.advance(ticks)
    }
}

/**
 * The curtain of the pause of a game that [paused] tells paused or not: shut at once when the screen shows a game
 * already paused (a recreation of the activity), with the effects of the theme.
 */
@Composable
internal fun rememberPauseCurtainState(paused: () -> Boolean): PauseCurtainState {
    val effects = BloxTheme.effects
    // Read once, without following the state: the screen that remembers it does not recompose as the game changes.
    return remember { PauseCurtainState(Snapshot.withoutReadObservation(paused), effects) }
}

/**
 * Moves the curtain of [state] as the game is [paused] or not, on the frame clock of Compose (ADR 0012, 0024): the
 * loop of the game is stopped while the curtain moves. Once Reprendre has opened it, the game goes on by [onResume].
 */
@Composable
internal fun PauseCurtainEffect(
    state: PauseCurtainState,
    paused: Boolean,
    onResume: () -> Unit,
) {
    val effects = BloxTheme.effects
    LaunchedEffect(state, paused) { state.follow(paused, effects) }
    val resume by rememberUpdatedState(onResume)
    LaunchedEffect(state) {
        snapshotFlow { state.curtain?.phase }.collectLatest { phase ->
            when (phase) {
                CurtainPhase.CLOSING, CurtainPhase.OPENING -> {
                    val ticks = FrameTicks()
                    while (true) withFrameNanos { state.advance(ticks.advance(it)) }
                }

                CurtainPhase.OPEN -> resume()

                CurtainPhase.CLOSED, null -> Unit
            }
        }
    }
}
