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
import ch.magori.blox.feature.composer.ComposerViewModel
import ch.magori.blox.feature.composer.navigation.ComposerRoute
import ch.magori.blox.feature.composer.navigation.composerEntry
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.game.navigation.gameEntry
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.home.navigation.homeEntry
import ch.magori.blox.feature.musics.DeletedMusicRelay
import ch.magori.blox.feature.musics.MyMusicsViewModel
import ch.magori.blox.feature.musics.navigation.MyMusicsRoute
import ch.magori.blox.feature.musics.navigation.myMusicsEntry

/**
 * Pile de navigation de l'application (ADR 0011). C'est le seul endroit qui connaît
 * plusieurs features : elles ne se connaissent pas entre elles. Naviguer, c'est ajouter
 * ou retirer une clé de la pile ; le retour système et le bouton Retour font la même chose.
 * Les deux décorateurs gardent l'état sauvegardable de chaque entrée et lient chaque ViewModel
 * à sa clé : une nouvelle partie, une nouvelle clé, un nouveau ViewModel. [deletedMusics] porte une musique supprimée
 * dans l'éditeur jusqu'à « Mes musiques », qui propose de l'annuler.
 */
@Composable
fun BloxNavDisplay(
    homeViewModelFactory: HomeViewModel.Factory,
    gameViewModelFactory: GameViewModel.Factory,
    composerViewModelFactory: ComposerViewModel.Factory,
    myMusicsViewModelFactory: MyMusicsViewModel.Factory,
    deletedMusics: DeletedMusicRelay,
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(HomeRoute),
) {
    // Never below the first screen, whatever asks to go back once more.
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
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
                // Points d'entrée de l'éditeur, une musique neuve, et de « Mes musiques » (V1).
                onCompose = { backStack.add(ComposerRoute()) },
                onMyMusics = { backStack.add(MyMusicsRoute) },
                viewModelFactory = homeViewModelFactory,
            )
            gameEntry(
                onBack = goBack,
                onReplay = { finished -> replay(backStack, finished) },
                viewModelFactory = gameViewModelFactory,
            )
            composerEntry(
                onBack = goBack,
                // Après une suppression, « Mes musiques » revient et propose de l'annuler (P4.19e).
                onDeleted = { deleted -> afterDeletion(backStack, deleted, deletedMusics::handOver) },
                viewModelFactory = composerViewModelFactory,
            )
            myMusicsEntry(
                onBack = goBack,
                // *Au hasard* ouvre l'éditeur sur une musique neuve tirée au hasard, *Nouvelle* sur une musique vierge.
                onRandom = { backStack.add(ComposerRoute(rollDice = true)) },
                onNew = { backStack.add(ComposerRoute()) },
                onEdit = { id -> backStack.add(ComposerRoute(musicKey = id.key)) },
                viewModelFactory = myMusicsViewModelFactory,
            )
        },
    )
}
