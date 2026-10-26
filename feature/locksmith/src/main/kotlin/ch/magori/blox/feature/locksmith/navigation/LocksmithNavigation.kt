package ch.magori.blox.feature.locksmith.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.locksmith.LocksmithScreen
import ch.magori.blox.feature.locksmith.LocksmithViewModel
import kotlinx.serialization.Serializable

/**
 * Clé de l'écran du Serrurier (vue V35, ADR 0025) : la serrure [level] (dès 1) de la graine [seed]. Une serrure = une
 * clé distincte, donc un ViewModel neuf ; « Serrure suivante » met une nouvelle clé à sa place, que `app` tire (le
 * ViewModel ne tire jamais lui-même la serrure suivante, ADR 0011 et 0025). Après la mort du processus, la clé restaurée
 * régénère la même serrure, sans pièce posée (ADR 0025).
 */
@Serializable
data class LocksmithRoute(val level: Int, val seed: Long) : NavKey {
    init {
        require(level >= 1) { "Padlocks are numbered from 1, got $level" }
    }

    /** The next padlock: one level up, on [seed], which `app` draws. */
    fun next(seed: Long): LocksmithRoute = LocksmithRoute(level + 1, seed)
}

/**
 * Déclare l'entrée du Serrurier (ADR 0011, 0025). `app` construit [viewModelFactory] ; la clé lui parvient par les
 * `CreationExtras` (ADR 0013). Le décorateur `rememberViewModelStoreNavEntryDecorator` de `NavDisplay` lie le
 * ViewModel à la clé.
 */
fun EntryProviderScope<NavKey>.locksmithEntry(viewModelFactory: LocksmithViewModel.Factory) {
    entry<LocksmithRoute> { route ->
        // The default extras of the owner (saved state, application…) are kept; the route joins them.
        val owner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner for the locksmith entry" }
        val defaults = (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
            ?: CreationExtras.Empty
        val extras = remember(route, defaults) {
            MutableCreationExtras(defaults).apply { set(LocksmithViewModel.RouteKey, route) }
        }
        val viewModel: LocksmithViewModel =
            viewModel(viewModelStoreOwner = owner, factory = viewModelFactory, extras = extras)
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LocksmithScreen(state = state)
    }
}
