package ch.magori.blox.feature.home.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.home.HomeScreen
import ch.magori.blox.feature.home.HomeViewModel
import kotlinx.serialization.Serializable

/** Clé de l'écran d'accueil (vue V1). */
@Serializable
data object HomeRoute : NavKey

/**
 * Déclare l'entrée Accueil dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] (ADR 0013) et reçoit, par [onPlay], le niveau de départ choisi : c'est `app` qui
 * crée la clé de la partie, avec sa graine.
 */
fun EntryProviderScope<NavKey>.homeEntry(
    onPlay: (startLevel: Int) -> Unit,
    viewModelFactory: HomeViewModel.Factory,
) {
    entry<HomeRoute> {
        val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        // Nothing is shown, and nothing can be played, until the saved settings are read: no theme flashes.
        uiState?.let { loaded ->
            HomeScreen(
                uiState = loaded,
                onThemeChosen = viewModel::onThemeChosen,
                onMusicChosen = viewModel::onMusicChosen,
                onStartLevelChosen = viewModel::onStartLevelChosen,
                onPlay = onPlay,
            )
        }
    }
}
