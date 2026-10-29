package ch.magori.blox.feature.locksmith.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.locksmith.PadlocksActions
import ch.magori.blox.feature.locksmith.PadlocksScreen
import ch.magori.blox.feature.locksmith.PadlocksViewModel
import kotlinx.serialization.Serializable

/**
 * Clé de l'écran « Serrures » (P7.35) : l'avancement du Serrurier, ouvert depuis la serrure [current] (dès 1), qu'il
 * dit « en cours », ou depuis aucune.
 */
@Serializable
data class PadlocksRoute(val current: Int? = null) : NavKey {
    init {
        require(current == null || current >= 1) { "Padlocks are numbered from 1, got $current" }
    }
}

/**
 * Déclare l'entrée de l'écran « Serrures » (ADR 0011). [onBack] est le bouton retour de la barre : `app` dépile.
 * [onOpen] reçoit la serrure touchée, une serrure ouverte ou la frontière, ou celle de Reprendre : `app` en fait la
 * navigation (P7.36). `app` construit [viewModelFactory] (ADR 0013).
 */
fun EntryProviderScope<NavKey>.padlocksEntry(
    onBack: () -> Unit,
    onOpen: (level: Int) -> Unit,
    viewModelFactory: PadlocksViewModel.Factory,
) {
    entry<PadlocksRoute> { route ->
        val viewModel: PadlocksViewModel = viewModel(factory = viewModelFactory)
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val actions = remember(onBack, onOpen) { PadlocksActions(onBack = onBack, onOpen = onOpen) }
        // Nothing is shown until the progress is read: no empty screen flashes.
        state?.let { loaded -> PadlocksScreen(state = loaded, current = route.current, actions = actions) }
    }
}
