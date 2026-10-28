package ch.magori.blox.feature.garden.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.garden.GardenActions
import ch.magori.blox.feature.garden.GardenScreen
import ch.magori.blox.feature.garden.GardenViewModel
import kotlinx.serialization.Serializable

/** Clé de l'écran du Jardin musical (vue V40, ADR 0027). Le jardin n'y est pas : il est gardé à part (P10.11). */
@Serializable
data object GardenRoute : NavKey

/**
 * Déclare l'entrée du Jardin musical (ADR 0011, 0027). `app` construit [viewModelFactory] (ADR 0013) et décide où mène
 * [onChangeGame] : le choix du jeu (ADR 0022).
 */
fun EntryProviderScope<NavKey>.gardenEntry(
    onChangeGame: () -> Unit,
    viewModelFactory: GardenViewModel.Factory,
) {
    entry<GardenRoute> {
        val viewModel: GardenViewModel = viewModel(factory = viewModelFactory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val actions = remember(onChangeGame) { GardenActions(onChangeGame = onChangeGame) }
        GardenScreen(uiState = uiState, actions = actions)
    }
}
