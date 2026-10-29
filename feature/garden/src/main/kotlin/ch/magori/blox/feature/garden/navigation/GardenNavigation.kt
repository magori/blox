package ch.magori.blox.feature.garden.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.designsystem.lifecycle.ScreenHiddenEffect
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
        ScreenHiddenEffect(onHidden = viewModel::onHidden)
        val actions = remember(onChangeGame, viewModel) { gardenActions(viewModel, onChangeGame) }
        GardenScreen(uiState = uiState, actions = actions)
    }
}

/** The actions of the screen, handed to [viewModel], Changer de jeu to `app` by [onChangeGame]. */
internal fun gardenActions(
    viewModel: GardenViewModel,
    onChangeGame: () -> Unit,
): GardenActions = GardenActions(
    onChangeGame = onChangeGame,
    onListen = viewModel::listen,
    onFrame = viewModel::onFrame,
    onRoll = viewModel::roll,
    onMood = viewModel::chooseMood,
    onClearAll = viewModel::clearAll,
    onTap = viewModel::tap,
    onChoose = viewModel::choose,
    onRemove = viewModel::remove,
    onTurn = viewModel::turn,
    onPlace = viewModel::place,
    onUndo = viewModel::undo,
    onOpenMyGardens = viewModel::openMyGardens,
    onCloseMyGardens = viewModel::closeMyGardens,
    onKeep = viewModel::startKeeping,
    onSaveInPlace = viewModel::saveInPlace,
    onNameChanged = viewModel::changeName,
    onCancelNaming = viewModel::cancelNaming,
    onConfirmKeeping = viewModel::confirmKeeping,
    onOpenKept = viewModel::openKept,
    onRename = viewModel::startRenaming,
    onConfirmRenaming = viewModel::confirmRenaming,
    onDelete = viewModel::startDeleting,
    onCancelDeleting = viewModel::cancelDeleting,
    onConfirmDeleting = viewModel::confirmDeleting,
)
