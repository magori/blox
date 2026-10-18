package ch.magori.blox.feature.game.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.magori.blox.feature.game.GameScreen
import kotlinx.serialization.Serializable

@Serializable
data object GameRoute

fun NavGraphBuilder.gameScreen(onBack: () -> Unit) {
    composable<GameRoute> {
        GameScreen(onBack = onBack)
    }
}
