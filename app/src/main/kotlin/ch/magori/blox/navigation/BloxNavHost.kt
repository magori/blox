package ch.magori.blox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.game.navigation.gameScreen
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.home.navigation.homeScreen

/**
 * Graphe de navigation de l'application. C'est le seul endroit qui connaît
 * plusieurs features : elles ne se connaissent pas entre elles.
 */
@Composable
fun BloxNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(onPlay = { navController.navigate(GameRoute) })
        gameScreen(onBack = { navController.popBackStack() })
    }
}
