package ch.magori.blox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.game.navigation.gameEntry
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.home.navigation.homeEntry

/**
 * Pile de navigation de l'application (ADR 0011). C'est le seul endroit qui connaît
 * plusieurs features : elles ne se connaissent pas entre elles. Naviguer, c'est ajouter
 * ou retirer une clé de la pile ; le retour système et le bouton Retour font la même chose.
 */
@Composable
fun BloxNavDisplay(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(HomeRoute),
) {
    val goBack: () -> Unit = { backStack.removeLastOrNull() }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = goBack,
        entryProvider = entryProvider {
            homeEntry(onPlay = { backStack.add(GameRoute) })
            gameEntry(onBack = goBack)
        },
    )
}
