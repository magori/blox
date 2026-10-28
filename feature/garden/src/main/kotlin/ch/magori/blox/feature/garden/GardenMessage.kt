package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import ch.magori.blox.core.garden.GardenRefusal

/**
 * The message under the grid of [state]: why the preview cannot be placed, off the grid before over a note, as the
 * garden judges it; else how to place it; else what can be done, on an empty garden or not.
 */
@StringRes
internal fun messageOf(state: GardenUiState): Int {
    val refusal = state.refusal
    return when {
        refusal == GardenRefusal.OUT_OF_GRID -> R.string.feature_garden_message_out_of_grid
        refusal == GardenRefusal.OVERLAP -> R.string.feature_garden_message_overlap
        state.aim != null -> R.string.feature_garden_message_preview
        state.garden.pieces.isEmpty() -> R.string.feature_garden_message_start
        else -> R.string.feature_garden_message_garden
    }
}
