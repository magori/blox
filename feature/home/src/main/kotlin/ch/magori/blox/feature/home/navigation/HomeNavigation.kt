package ch.magori.blox.feature.home.navigation

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.home.HomeScreen
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.SettingsActions
import ch.magori.blox.feature.home.SettingsScreen
import ch.magori.blox.feature.home.SettingsViewModel
import ch.magori.blox.feature.home.rememberMusicFilePicker
import kotlinx.serialization.Serializable

/** Clé de l'écran d'accueil (vue V1). */
@Serializable
data object HomeRoute : NavKey

/**
 * Déclare l'entrée Accueil dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] (ADR 0013) et reçoit, par [onPlay], le niveau de départ choisi : c'est `app` qui
 * crée la clé de la partie, avec sa graine. [onCompose] ouvre l'éditeur de musique et [onMyMusics] la liste « Mes
 * musiques », que l'accueil ne connaît pas ; [onSettings] ouvre la page Paramètres ([SettingsRoute]).
 */
fun EntryProviderScope<NavKey>.homeEntry(
    onPlay: (startLevel: Int) -> Unit,
    onCompose: () -> Unit,
    onMyMusics: () -> Unit,
    onSettings: () -> Unit,
    viewModelFactory: HomeViewModel.Factory,
) {
    entry<HomeRoute> {
        val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        // The view model decides what each event of the screen means for the music listened to (tested
        // there); this observer, without a JVM test, only hands the events over.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, viewModel) {
            val observer = LifecycleEventObserver { _, event -> viewModel.onLifecycleEvent(event) }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        // The view model decides when the picker of the system opens (tested there); the screen opens it, and hands
        // the file picked, if any, over.
        val pickMusicFile = rememberMusicFilePicker(onPicked = viewModel::onMusicFileChosen)
        val pickerRequested by viewModel.musicFilePickerRequested.collectAsStateWithLifecycle()
        LaunchedEffect(pickerRequested) {
            if (pickerRequested) {
                viewModel.onMusicFilePickerOpened()
                pickMusicFile()
            }
        }
        // Nothing is shown, and nothing can be played, until the saved settings are read: no theme flashes.
        uiState?.let { loaded ->
            HomeScreen(
                uiState = loaded,
                onThemeChosen = viewModel::onThemeChosen,
                onMusicChosen = viewModel::onMusicChosen,
                onListen = viewModel::onListen,
                onListenGameMusic = viewModel::onListenGameMusic,
                onChangeMusicFile = viewModel::onChangeMusicFile,
                onStartLevelChosen = viewModel::onStartLevelChosen,
                onPlay = { startLevel ->
                    viewModel.onPlay()
                    onPlay(startLevel)
                },
                onCompose = {
                    viewModel.onCompose()
                    onCompose()
                },
                onMyMusics = {
                    viewModel.onMyMusics()
                    onMyMusics()
                },
                onSettings = {
                    viewModel.onSettings()
                    onSettings()
                },
            )
        }
    }
}

/** Clé de la page Paramètres (vue V21). */
@Serializable
data object SettingsRoute : NavKey

/**
 * Déclare l'entrée Paramètres (ADR 0011). `app` construit [viewModelFactory] (ADR 0013) ; [onBack] revient à
 * l'accueil.
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
