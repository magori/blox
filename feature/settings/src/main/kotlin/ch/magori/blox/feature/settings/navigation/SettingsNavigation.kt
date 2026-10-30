package ch.magori.blox.feature.settings.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.settings.SettingsActions
import ch.magori.blox.feature.settings.SettingsScreen
import ch.magori.blox.feature.settings.SettingsViewModel
import kotlinx.serialization.Serializable

/** Clé de la page Paramètres (vue V21). */
@Serializable
data object SettingsRoute : NavKey

/**
 * Déclare l'entrée Paramètres (ADR 0011). `app` construit [viewModelFactory] (ADR 0013) ; [onBack] revient à
 * l'écran qui l'a ouverte.
 */
fun EntryProviderScope<NavKey>.settingsEntry(
    onBack: () -> Unit,
    viewModelFactory: SettingsViewModel.Factory,
) {
    entry<SettingsRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        // A second tap on the way back does nothing.
        val goBack = dropUnlessResumed { onBack() }
        val actions = remember(viewModel, goBack) {
            SettingsActions(
                onBack = goBack,
                onReducedEffectsChanged = viewModel::onReducedEffectsChanged,
                onLeftHandedChanged = viewModel::onLeftHandedChanged,
                onSoundEffectsChanged = viewModel::onSoundEffectsChanged,
                onEffectsVolumeChanged = viewModel::onEffectsVolumeChanged,
                onMusicVolumeChanged = viewModel::onMusicVolumeChanged,
                onVibrationsChanged = viewModel::onVibrationsChanged,
                onKeyVibrationChanged = viewModel::onKeyVibrationChanged,
                onClearRecord = viewModel::onClearRecord,
                onKeepRecord = viewModel::onKeepRecord,
                onClearRecordConfirmed = viewModel::onClearRecordConfirmed,
            )
        }
        // The page and its header show at once; its settings, once the saved settings are read (no switch flashes off).
        SettingsScreen(uiState = uiState, actions = actions)
    }
}
