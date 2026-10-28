package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.garden.Garden

/** The screen of the musical garden (V40): the [garden] under way, its mood with it. */
@Immutable
internal data class GardenUiState(val garden: Garden)
