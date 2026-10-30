package ch.magori.blox.feature.game.topbar

import androidx.compose.runtime.Immutable
import ch.magori.blox.feature.game.GameUiState

/**
 * The best the top bar shows (the record of the Classic, or the best of the day): its [value], null while unknown, and
 * whether it is a new best, [beaten].
 */
@Immutable
internal data class ShownBest(val value: Int?, val beaten: Boolean)

/** The best of [this] game, as the top bar shows it: once beaten, the score, which is the new best. */
internal fun GameUiState.shownBest(): ShownBest =
    if (bestBeaten) ShownBest(value = score.value, beaten = true) else ShownBest(value = best, beaten = false)
