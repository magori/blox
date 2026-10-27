package ch.magori.blox.feature.composer.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewmodel.CreationExtras
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
 * musique neuve. Avec [rollDice] (*Au hasard* de « Mes musiques »), le dé est lancé dès l'ouverture, sur Surprise ;
 * une entrée restaurée après la mort du processus garde la musique tirée. Le dé ne vaut que pour une musique neuve :
 * [rollDice] avec une [musicKey] est refusé.
 */
@Serializable
data class ComposerRoute(val musicKey: String? = null, val rollDice: Boolean = false) : NavKey {
    init {
        require(musicKey == null || !rollDice) {
            "The dice rolls on a new music only, not on the saved music $musicKey"
        }
    }
}

/**
 * Déclare l'entrée Éditeur dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] (ADR 0013) ; la clé de la musique et le dé lui parviennent par les `CreationExtras`. [onBack]
 * quitte l'éditeur ; [onDeleted] le quitte une fois sa musique supprimée, avec de quoi la restaurer (« Annuler » de
 * « Mes musiques »).
 */
fun EntryProviderScope<NavKey>.composerEntry(
    onBack: () -> Unit,
    onDeleted: (DeletedMusic) -> Unit,
    viewModelFactory: ComposerViewModel.Factory,
) {
    entry<ComposerRoute> { route ->
        // The default extras of the entry (saved state, application…) are kept; the key of the music and the dice join
        // them (ADR 0013).
        val defaults = LocalViewModelStoreOwner.current.defaultViewModelCreationExtras
        val extras = CreationExtras(defaults) {
            route.musicKey?.let { set(ComposerViewModel.MusicKey, it) }
            if (route.rollDice) set(ComposerViewModel.RollDice, true)
        }
        val viewModel: ComposerViewModel = viewModel(factory = viewModelFactory, extras = extras)
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
