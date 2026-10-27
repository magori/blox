package ch.magori.blox.feature.daily.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.daily.DailyActions
import ch.magori.blox.feature.daily.DailyScreen
import ch.magori.blox.feature.daily.DailyViewModel
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Clé de l'écran d'entrée de la Graine du jour (vue V38). La date n'y est pas : l'écran la relit à chaque affichage. */
@Serializable
data object DailyRoute : NavKey

/**
 * Déclare l'entrée de la Graine du jour (ADR 0011). `app` construit [viewModelFactory] (ADR 0013) et décide où mène
 * chaque action, que l'écran ne connaît pas : [onPlay] ouvre la partie de la date affichée (ses pièces sont celles
 * montrées), dont `app` tire la clé ; [onSettings] la page Paramètres ; [onChangeGame] revient au choix du jeu
 * (ADR 0022) ; [onCopyScore] copie le score de la date affichée (P9.6).
 */
fun EntryProviderScope<NavKey>.dailyEntry(
    onPlay: (date: LocalDate) -> Unit,
    onSettings: () -> Unit,
    onChangeGame: () -> Unit,
    onCopyScore: (date: LocalDate) -> Unit,
    viewModelFactory: DailyViewModel.Factory,
) {
    entry<DailyRoute> {
        val viewModel: DailyViewModel = viewModel(factory = viewModelFactory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        // Each time the screen shows, or the application comes back to the foreground, the date is read again (tested
        // in the view model): a new day gives its seed, without a timer until midnight.
        LifecycleStartEffect(viewModel) {
            viewModel.onShown()
            onStopOrDispose {}
        }
        val actions = remember(onPlay, onSettings, onChangeGame, onCopyScore) {
            DailyActions(
                onChangeGame = onChangeGame,
                onSettings = onSettings,
                onPlay = onPlay,
                onCopyScore = onCopyScore,
            )
        }
        // Nothing is shown until the results are read: no « — » flashes before them.
        uiState?.let { loaded -> DailyScreen(uiState = loaded, actions = actions) }
    }
}
