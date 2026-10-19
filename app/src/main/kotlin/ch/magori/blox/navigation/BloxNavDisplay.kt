package ch.magori.blox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.game.navigation.gameEntry
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.home.navigation.homeEntry

/**
 * Pile de navigation de l'application (ADR 0011). C'est le seul endroit qui connaît
 * plusieurs features : elles ne se connaissent pas entre elles. Naviguer, c'est ajouter
 * ou retirer une clé de la pile ; le retour système et le bouton Retour font la même chose.
 * Les deux décorateurs gardent l'état sauvegardable de chaque entrée et lient chaque ViewModel
 * à sa clé : une nouvelle partie, une nouvelle clé, un nouveau ViewModel.
 */
@Composable
fun BloxNavDisplay(
    homeViewModelFactory: HomeViewModel.Factory,
    gameViewModelFactory: GameViewModel.Factory,
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(HomeRoute),
) {
    val goBack: () -> Unit = { backStack.removeLastOrNull() }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = goBack,
        entryDecorators = listOf(
            // L'ordre compte : le décorateur saveable fournit le registre d'état propre à l'entrée, d'où le décorateur
            // de ViewModel tire le SavedStateHandle de la partie (P5.13).
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            homeEntry(
                onPlay = { startLevel -> backStack.add(newGameRoute(startLevel)) },
                viewModelFactory = homeViewModelFactory,
            )
            gameEntry(
                onBack = goBack,
                onReplay = { finished -> replay(backStack, finished) },
                viewModelFactory = gameViewModelFactory,
            )
        },
    )
}
