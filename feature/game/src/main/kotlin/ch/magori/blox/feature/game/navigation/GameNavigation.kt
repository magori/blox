package ch.magori.blox.feature.game.navigation

import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.game.GameScreen
import ch.magori.blox.feature.game.GameViewModel
import kotlinx.serialization.Serializable

/**
 * Clé de l'écran de jeu (vues V2 et V3) : une partie, de son niveau de départ et de la graine
 * de son tirage. Une partie = une clé distincte, donc un ViewModel neuf ; « Rejouer » mettra
 * une nouvelle clé, le ViewModel ne relance jamais de partie lui-même (ADR 0011).
 */
@Serializable
data class GameRoute(val startLevel: Int, val seed: Long) : NavKey {
    init {
        require(startLevel >= 0) { "A start level cannot be negative: $startLevel" }
    }
}

/**
 * Déclare l'entrée Jeu dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] ; la clé lui parvient par les `CreationExtras` (ADR 0013). [onReplay] reçoit la
 * partie terminée : `app` met une nouvelle clé à sa place (ADR 0011). Le décorateur
 * `rememberViewModelStoreNavEntryDecorator` de `NavDisplay` lie le ViewModel à la clé.
 */
fun EntryProviderScope<NavKey>.gameEntry(
    onBack: () -> Unit,
    onReplay: (finished: GameRoute) -> Unit,
    viewModelFactory: GameViewModel.Factory,
) {
    entry<GameRoute> { route ->
        // The default extras of the entry (saved state, application…) are kept; the route joins them (ADR 0013).
        val defaults = LocalViewModelStoreOwner.current.defaultViewModelCreationExtras
        val extras = CreationExtras(defaults) { set(GameViewModel.RouteKey, route) }
        val viewModel: GameViewModel = viewModel(factory = viewModelFactory, extras = extras)
        GameScreen(viewModel = viewModel, onBack = onBack, onReplay = { onReplay(route) })
    }
}
