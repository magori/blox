package ch.magori.blox.feature.composer.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.composer.ComposerScreen
import ch.magori.blox.feature.composer.ComposerViewModel
import kotlinx.serialization.Serializable

/** Clé de l'éditeur de musique (vues V8 et V11), ouvert sur une musique neuve. */
@Serializable
data object ComposerRoute : NavKey

/**
 * Déclare l'entrée Éditeur dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] (ADR 0013) ; [onBack] quitte l'éditeur.
 */
fun EntryProviderScope<NavKey>.composerEntry(
    onBack: () -> Unit,
    viewModelFactory: ComposerViewModel.Factory,
) {
    entry<ComposerRoute> {
        val viewModel: ComposerViewModel = viewModel(factory = viewModelFactory)
        ComposerScreen(viewModel = viewModel, onBack = onBack)
    }
}
