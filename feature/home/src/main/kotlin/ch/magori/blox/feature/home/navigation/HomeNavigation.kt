package ch.magori.blox.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.magori.blox.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(onPlay: () -> Unit) {
    composable<HomeRoute> {
        HomeScreen(onPlay = onPlay)
    }
}
