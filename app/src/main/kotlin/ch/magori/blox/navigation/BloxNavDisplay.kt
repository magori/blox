package ch.magori.blox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ch.magori.blox.daily.NewCartridges
import ch.magori.blox.daily.dailyDateOf
import ch.magori.blox.daily.newDailyGameRoute
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.cartridges.navigation.cartridgesEntry
import ch.magori.blox.feature.composer.ComposerViewModel
import ch.magori.blox.feature.composer.navigation.ComposerRoute
import ch.magori.blox.feature.composer.navigation.composerEntry
import ch.magori.blox.feature.daily.DailyViewModel
import ch.magori.blox.feature.daily.navigation.dailyEntry
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.game.navigation.gameEntry
import ch.magori.blox.feature.garden.GardenViewModel
import ch.magori.blox.feature.garden.navigation.gardenEntry
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.navigation.homeEntry
import ch.magori.blox.feature.locksmith.LocksmithViewModel
import ch.magori.blox.feature.locksmith.PadlocksViewModel
import ch.magori.blox.feature.locksmith.navigation.locksmithEntry
import ch.magori.blox.feature.locksmith.navigation.padlocksEntry
import ch.magori.blox.feature.musics.DeletedMusicRelay
import ch.magori.blox.feature.musics.MyMusicsViewModel
import ch.magori.blox.feature.musics.navigation.MyMusicsRoute
import ch.magori.blox.feature.musics.navigation.myMusicsEntry
import ch.magori.blox.feature.settings.SettingsViewModel
import ch.magori.blox.feature.settings.navigation.SettingsRoute
import ch.magori.blox.feature.settings.navigation.SettingsSection
import ch.magori.blox.feature.settings.navigation.settingsEntry
import ch.magori.blox.locksmith.PadlockFrontier
import java.time.LocalDate

/**
 * Pile de navigation de l'application (ADR 0011). C'est le seul endroit qui connaît
 * plusieurs features : elles ne se connaissent pas entre elles. Naviguer, c'est ajouter
 * ou retirer une clé de la pile ; le retour système et le bouton Retour font la même chose.
 * Les deux décorateurs gardent l'état sauvegardable de chaque entrée et lient chaque ViewModel
 * à sa clé : une nouvelle partie, une nouvelle clé, un nouveau ViewModel. [deletedMusics] porte une musique supprimée
 * dans l'éditeur jusqu'à « Mes musiques », qui propose de l'annuler ; [newCartridges] les cartouches qui portent
 * l'autocollant « NOUVELLE » (la Graine du jour avant le premier essai du jour) ; [onCopyDailyScore] copie le score de
 * la Graine du jour d'une date, celle de l'écran d'entrée ou de la partie terminée (P9.6) ; [padlockFrontier] la
 * frontière du Serrurier, que sa cartouche nomme et ouvre, lue au toucher (P7.34). La pile s'ouvre sur le choix
 * du jeu ([CartridgesRoute], ADR 0022) : le retour système, là, quitte l'application.
 */
@Composable
fun BloxNavDisplay(
    homeViewModelFactory: HomeViewModel.Factory,
    gameViewModelFactory: GameViewModel.Factory,
    composerViewModelFactory: ComposerViewModel.Factory,
    myMusicsViewModelFactory: MyMusicsViewModel.Factory,
    settingsViewModelFactory: SettingsViewModel.Factory,
    deletedMusics: DeletedMusicRelay,
    locksmithViewModelFactory: LocksmithViewModel.Factory,
    dailyViewModelFactory: DailyViewModel.Factory,
    onCopyDailyScore: (date: LocalDate) -> Unit,
    newCartridges: NewCartridges,
    gardenViewModelFactory: GardenViewModel.Factory,
    padlockFrontier: PadlockFrontier,
    padlocksViewModelFactory: PadlocksViewModel.Factory,
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(CartridgesRoute),
) {
    // Never below the first screen, whatever asks to go back once more.
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
    // Where a tap on the Locksmith cartridge awaits its frontier, for as long as the navigation shows.
    val scope = rememberCoroutineScope()
    val cartridges =
        remember(backStack, scope, padlockFrontier) { CartridgeOpener(backStack, scope, padlockFrontier::await) }
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
            cartridgesEntry(
                newCartridges = newCartridges.updates,
                currentNewCartridges = newCartridges::current,
                padlockFrontier = padlockFrontier.current,
                onOpen = cartridges::open,
                onSettings = cartridges::openSettings,
            )
            homeEntry(
                onPlay = { startLevel -> backStack.add(newGameRoute(startLevel)) },
                // Points d'entrée de l'éditeur, une musique neuve, et de « Mes musiques » (V1).
                onCompose = { backStack.add(ComposerRoute()) },
                onMyMusics = { backStack.add(MyMusicsRoute) },
                onSettings = { backStack.add(SettingsRoute(SettingsSection.CLASSIC)) },
                onChangeGame = { changeGame(backStack) },
                viewModelFactory = homeViewModelFactory,
            )
            gameEntry(
                onBack = goBack,
                onReplay = { finished -> replay(backStack, finished) },
                // The score of the date of the game, even replayed after midnight; only a daily game offers Copier.
                onCopy = { finished -> dailyDateOf(finished)?.let(onCopyDailyScore) },
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
            settingsEntry(onBack = goBack, viewModelFactory = settingsViewModelFactory)
            locksmithEntry(
                onBack = goBack,
                onNext = { open -> nextPadlock(backStack, open) },
                onResume = { open, frontier -> resumePadlocks(backStack, open, frontier) },
                onPadlocks = { open -> openPadlocks(backStack, open) },
                viewModelFactory = locksmithViewModelFactory,
            )
            // The screen « Serrures », over the padlock whose disc opened it (P7.35, P7.36): back finds that padlock as
            // it was left; a padlock chosen there takes its place, or only goes back to it when it is the same. Once the
            // progress is cleared there (P7.37), padlock 1 comes back over the cartridges.
            padlocksEntry(
                onBack = goBack,
                onOpen = { list, chosen -> choosePadlock(backStack, list, chosen) },
                onCleared = { list -> progressCleared(backStack, list) },
                viewModelFactory = padlocksViewModelFactory,
            )
            dailyEntry(
                // The game of the date shown, whose pieces the entry shows, even once the phone passed midnight.
                onPlay = { date -> backStack.add(newDailyGameRoute(date)) },
                // The section of the Classic, its record within, as before P12.3: P12.6 opens the page without it.
                onSettings = { backStack.add(SettingsRoute(SettingsSection.CLASSIC)) },
                onChangeGame = { changeGame(backStack) },
                onCopyScore = onCopyDailyScore,
                viewModelFactory = dailyViewModelFactory,
            )
            gardenEntry(onChangeGame = { changeGame(backStack) }, viewModelFactory = gardenViewModelFactory)
        },
    )
}
