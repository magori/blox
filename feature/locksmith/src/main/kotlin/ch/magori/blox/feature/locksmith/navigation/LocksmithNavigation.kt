package ch.magori.blox.feature.locksmith.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.locksmith.LocksmithActions
import ch.magori.blox.feature.locksmith.LocksmithScreen
import ch.magori.blox.feature.locksmith.LocksmithViewModel
import kotlinx.serialization.Serializable

/**
 * Clé de l'écran du Serrurier (vue V35, ADR 0025) : la serrure [level] (dès 1), la même pour tous (`Padlock.ofLevel`).
 * Une serrure = une clé distincte, donc un ViewModel neuf ; « Serrure suivante » met la clé suivante à sa place, dans
 * `app` (le ViewModel ne change jamais lui-même de serrure, ADR 0011 et 0025). Après la mort du processus, la clé
 * restaurée redonne la même serrure, sans pièce posée (ADR 0025).
 */
@Serializable
data class LocksmithRoute(val level: Int) : NavKey {
    init {
        require(level >= 1) { "Padlocks are numbered from 1, got $level" }
    }

    /** The next padlock: one level up. */
    fun next(): LocksmithRoute = LocksmithRoute(level + 1)
}

/**
 * Déclare l'entrée du Serrurier (ADR 0011, 0025). [onBack] est le bouton retour de la barre : `app` dépile.
 * [onNext] reçoit la serrure ouverte : `app` met la suivante à sa place. `app` construit [viewModelFactory] ; la clé
 * lui parvient par les `CreationExtras` (ADR 0013). Le décorateur `rememberViewModelStoreNavEntryDecorator` de
 * `NavDisplay` lie le ViewModel à la clé.
 */
fun EntryProviderScope<NavKey>.locksmithEntry(
    onBack: () -> Unit,
    onNext: (open: LocksmithRoute) -> Unit,
    viewModelFactory: LocksmithViewModel.Factory,
) {
    entry<LocksmithRoute> { route ->
        // The default extras of the entry (saved state, application…) are kept; the route joins them (ADR 0013).
        val defaults = LocalViewModelStoreOwner.current.defaultViewModelCreationExtras
        val extras = CreationExtras(defaults) { set(LocksmithViewModel.RouteKey, route) }
        val viewModel: LocksmithViewModel = viewModel(factory = viewModelFactory, extras = extras)
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LocksmithScreen(
            state = state,
            actions = remember(viewModel, route, onBack, onNext) {
                LocksmithActions(
                    onBack = onBack,
                    onTap = viewModel::tap,
                    onSelect = viewModel::select,
                    onTakeBack = viewModel::takeBack,
                    onTurn = viewModel::turn,
                    onPlace = viewModel::placeAim,
                    onHint = viewModel::hint,
                    onRestart = viewModel::restart,
                    onNext = { onNext(route) },
                )
            },
        )
    }
}
