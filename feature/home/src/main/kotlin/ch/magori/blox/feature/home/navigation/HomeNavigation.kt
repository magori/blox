package ch.magori.blox.feature.home.navigation

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.home.HomeScreen
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.rememberMusicFilePicker
import kotlinx.serialization.Serializable

/** Clé de l'écran d'accueil (vue V1). */
@Serializable
data object HomeRoute : NavKey

/**
 * Déclare l'entrée Accueil dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] (ADR 0013) et reçoit, par [onPlay], le niveau de départ choisi : c'est `app` qui
 * crée la clé de la partie, avec sa graine. [onCompose] ouvre l'éditeur de musique, que l'accueil ne connaît pas.
 */
fun EntryProviderScope<NavKey>.homeEntry(
    onPlay: (startLevel: Int) -> Unit,
    onCompose: () -> Unit,
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
            )
        }
    }
}
