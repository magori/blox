package ch.magori.blox.feature.game.gameover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import kotlinx.coroutines.flow.collectLatest

/** The game over of a game screen as it plays, [play], none while the game goes on. */
@Stable
internal class GameOverPlayState(private val over: Boolean) {
    var play: GameOverPlay? by mutableStateOf(null)
        private set

    /**
     * The game is over, as [gameOver] says, or not: its game over starts to play, with [effects]; a game shown already
     * [over] shows its game over played to its end.
     */
    fun follow(
        gameOver: GameOverUiState?,
        effects: BloxEffects,
    ) {
        play = gameOver?.let { play ?: GameOverPlay.starting(effects, it.newBest).endedIf(over) }
    }

    /** The game over [ticks] later. */
    fun advance(ticks: Int) {
        play = play?.advance(ticks)
    }
}

/** [this] game over, played to its end if [ended]. */
private fun GameOverPlay.endedIf(ended: Boolean): GameOverPlay = if (ended) ended() else this

/**
 * The game over of a game that [over] tells over or not: played to its end at once when the screen shows a game already
 * over (a recreation of the activity).
 */
@Composable
internal fun rememberGameOverPlayState(over: () -> Boolean): GameOverPlayState =
    // Read once, without following the state: the screen that remembers it does not recompose as the game changes.
    remember { GameOverPlayState(Snapshot.withoutReadObservation(over)) }

/**
 * Plays the game over of [state] once the game is over, as [gameOver] says, on the frame clock of Compose (ADR 0012,
 * 0024): the loop of the game is stopped. No frame once it has played.
 */
@Composable
internal fun GameOverPlayEffect(
    state: GameOverPlayState,
    gameOver: GameOverUiState?,
) {
    val effects = BloxTheme.effects
    LaunchedEffect(state, gameOver) { state.follow(gameOver, effects) }
    LaunchedEffect(state) {
        val ticks = FrameTicks()
        snapshotFlow { state.play?.playing == true }.collectLatest { playing ->
            if (playing) {
                ticks.reset()
                while (true) withFrameNanos { state.advance(ticks.advance(it)) }
            }
        }
    }
}
