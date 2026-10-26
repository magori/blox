package ch.magori.blox.feature.game.topbar

import androidx.compose.runtime.Immutable
import ch.magori.blox.feature.game.GameUiState

/** The record the top bar shows: its [value], null while unknown, and whether it is a new record, [beaten]. */
@Immutable
internal data class ShownRecord(val value: Int?, val beaten: Boolean)

/** The record of [this] game, as the top bar shows it: once beaten, the score, which is the new record. */
internal fun GameUiState.shownRecord(): ShownRecord =
    if (recordBeaten) ShownRecord(value = score.value, beaten = true) else ShownRecord(value = record, beaten = false)
