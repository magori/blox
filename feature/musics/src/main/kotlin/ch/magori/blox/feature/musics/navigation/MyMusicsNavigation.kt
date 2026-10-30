package ch.magori.blox.feature.musics.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.designsystem.lifecycle.ScreenHiddenEffect
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.musics.MyMusicsActions
import ch.magori.blox.feature.musics.MyMusicsScreen
import ch.magori.blox.feature.musics.MyMusicsViewModel
import kotlinx.serialization.Serializable

/** Clé de « Mes musiques » (vues V7 et V10). */
@Serializable
data object MyMusicsRoute : NavKey

/**
 * Déclare l'entrée « Mes musiques » dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] (ADR 0013) ; [onBack] revient à l'accueil ; [onNew] ouvre l'éditeur sur une musique neuve,
 * [onRandom] sur une musique tirée au hasard et [onEdit] sur la musique enregistrée de cet identifiant : l'éditeur est
 * une autre feature, que la liste ne connaît pas.
 */
fun EntryProviderScope<NavKey>.myMusicsEntry(
    onBack: () -> Unit,
    onRandom: () -> Unit,
    onNew: () -> Unit,
    onEdit: (ScoreId.Personal) -> Unit,
    viewModelFactory: MyMusicsViewModel.Factory,
) {
    entry<MyMusicsRoute> {
        val viewModel: MyMusicsViewModel = viewModel(factory = viewModelFactory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ScreenHiddenEffect(onHidden = viewModel::onHidden)
        // Taps that leave count only while this screen is resumed: once the editor is on its way, a second does nothing.
        val goBack = dropUnlessResumed { onBack() }
        val openRandom = dropUnlessResumed { onRandom() }
        val openNew = dropUnlessResumed { onNew() }
        // The same for a tap that opens a music, which dropUnlessResumed cannot carry: it takes no identity.
        val currentEdit by rememberUpdatedState(onEdit)
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val openEdit: (ScoreId.Personal) -> Unit = remember(lifecycle) {
            { id -> if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) currentEdit(id) }
        }
        val actions = remember(viewModel, goBack, openRandom, openNew, openEdit) {
            EntryActions(viewModel, goBack, openRandom, openNew, openEdit)
        }
        // Nothing is shown until the musics are read: no empty list flashes.
        uiState?.let { loaded -> MyMusicsScreen(uiState = loaded, actions = actions) }
    }
}

/** The actions of the screen: to the [viewModel], and to the navigation of `app` for the editor and home. */
private class EntryActions(
    private val viewModel: MyMusicsViewModel,
    private val goBack: () -> Unit,
    private val openRandom: () -> Unit,
    private val openNew: () -> Unit,
    private val openEdit: (ScoreId.Personal) -> Unit,
) : MyMusicsActions {
    override fun onBack() = goBack()

    override fun onListen(id: ScoreId.Personal) = viewModel.onListen(id)

    override fun onUse(id: ScoreId.Personal) = viewModel.onUse(id)

    override fun onEdit(id: ScoreId.Personal) {
        viewModel.onEditorOpened()
        openEdit(id)
    }

    override fun onRename(id: ScoreId.Personal) = viewModel.onRename(id)

    override fun renameProblem(name: String): NameProblem? = viewModel.renameProblem(name)

    override fun onRenamed(name: String) = viewModel.onRenamed(name)

    override fun onDelete(id: ScoreId.Personal) = viewModel.onDelete(id)

    override fun onDeleteConfirmed() = viewModel.onDeleteConfirmed()

    override fun onDialogDismissed() = viewModel.onDialogDismissed()

    override fun onUndo() = viewModel.onUndo()

    override fun onUndoDismissed() = viewModel.onUndoDismissed()

    override fun onRandom() {
        viewModel.onEditorOpened()
        openRandom()
    }

    override fun onNew() {
        viewModel.onEditorOpened()
        openNew()
    }
}
