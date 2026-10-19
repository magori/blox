package ch.magori.blox.feature.composer.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.feature.composer.ComposerScreen
import ch.magori.blox.feature.composer.ComposerViewModel
import kotlinx.serialization.Serializable

/**
 * Clé de l'éditeur de musique (vues V8 et V11) : la musique enregistrée de clé [musicKey] (`ScoreId.Personal.key` de
 * `SavedMusic.id`), ou une musique neuve quand elle est absente. Une clé que la bibliothèque ne connaît pas ouvre une
 * musique neuve.
 */
@Serializable
data class ComposerRoute(val musicKey: String? = null) : NavKey

/**
 * Déclare l'entrée Éditeur dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] (ADR 0013) ; la clé de la musique lui parvient par les `CreationExtras`. [onBack] quitte
 * l'éditeur ; [onDeleted] le quitte une fois sa musique supprimée, avec de quoi la restaurer (« Annuler » de
 * « Mes musiques »).
 */
fun EntryProviderScope<NavKey>.composerEntry(
    onBack: () -> Unit,
    onDeleted: (DeletedMusic) -> Unit,
    viewModelFactory: ComposerViewModel.Factory,
) {
    entry<ComposerRoute> { route ->
        // The default extras of the owner (saved state, application…) are kept; the key of the music joins them.
        val owner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner for the composer entry" }
        val defaults = (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
            ?: CreationExtras.Empty
        val extras = remember(route, defaults) {
            MutableCreationExtras(defaults).apply { route.musicKey?.let { set(ComposerViewModel.MusicKey, it) } }
        }
        val viewModel: ComposerViewModel =
            viewModel(viewModelStoreOwner = owner, factory = viewModelFactory, extras = extras)
        // Leaving counts only while the editor is resumed: once it is on its way out, a second exit pops nothing more.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val currentBack by rememberUpdatedState(onBack)
        val currentDeleted by rememberUpdatedState(onDeleted)
        val resumed = { lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        ComposerScreen(
            viewModel = viewModel,
            onBack = { if (resumed()) currentBack() },
            onDeleted = { if (resumed()) currentDeleted(it) },
        )
    }
}
