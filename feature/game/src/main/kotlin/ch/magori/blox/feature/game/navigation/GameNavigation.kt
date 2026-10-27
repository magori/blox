package ch.magori.blox.feature.game.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.feature.game.GameScreen
import ch.magori.blox.feature.game.GameViewModel
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.random.Random

/**
 * The key of the game screen (views V2 and V3): one game, from its [startLevel], the [seed] of its draw and its
 * [variant] (ADR 0026). One game, one key, so a new view model; « Rejouer » puts a new key in its place
 * ([replayed]), the view model never starts a game again by itself (ADR 0011).
 */
@Serializable
data class GameRoute(val startLevel: Int, val seed: Long, val variant: GameVariant = GameVariant.Classic) : NavKey {
    init {
        require(startLevel >= 0) { "A start level cannot be negative: $startLevel" }
        require(variant !is GameVariant.Daily || startLevel == DAILY_START_LEVEL) {
            "A daily game starts at level $DAILY_START_LEVEL, not $startLevel"
        }
    }

    /** The game « Rejouer » puts in place of this one, once over, as its variant says, from [random]. */
    fun replayed(random: Random): GameRoute = variant.replay(this, random)

    companion object {
        /**
         * The daily game of [date], whose [seed] `app` gives (`DailySeed`), at level 5 whatever the level chosen on the
         * home screen; [id] tells apart two games of the same date (docs/graine-du-jour.md).
         */
        fun daily(
            date: LocalDate,
            seed: Long,
            id: Long,
        ): GameRoute = GameRoute(DAILY_START_LEVEL, seed, GameVariant.Daily(date, id))

        /** The start level imposed on the daily game (docs/graine-du-jour.md). */
        private const val DAILY_START_LEVEL = 5
    }
}

/**
 * Déclare l'entrée Jeu dans le fournisseur d'entrées de l'application (ADR 0011). `app` construit
 * [viewModelFactory] ; la clé lui parvient par les `CreationExtras` (ADR 0013). [onReplay] reçoit la
 * partie terminée : `app` met une nouvelle clé à sa place (ADR 0011). Le décorateur
 * `rememberViewModelStoreNavEntryDecorator` de `NavDisplay` lie le ViewModel à la clé.
 */
fun EntryProviderScope<NavKey>.gameEntry(
    onBack: () -> Unit,
    onReplay: (finished: GameRoute) -> Unit,
    viewModelFactory: GameViewModel.Factory,
) {
    entry<GameRoute> { route ->
        // The default extras of the entry (saved state, application…) are kept; the route joins them (ADR 0013).
        val defaults = LocalViewModelStoreOwner.current.defaultViewModelCreationExtras
        val extras = CreationExtras(defaults) { set(GameViewModel.RouteKey, route) }
        val viewModel: GameViewModel = viewModel(factory = viewModelFactory, extras = extras)
        // The Classic follows the theme chosen; the daily game is drawn in the Colorful theme only (ADR 0026).
        GameTheme(route.variant) {
            GameScreen(viewModel = viewModel, onBack = onBack, onReplay = { onReplay(route) })
        }
    }
}

/**
 * [content] in the theme of a game of [variant]: the Classic follows the theme around it, the theme chosen; a daily
 * game is drawn in the Colorful theme only, in its variant for the mode of the system, [darkSystem] (ADR 0026).
 */
@Composable
internal fun GameTheme(
    variant: GameVariant,
    darkSystem: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    if (variant.colorfulOnly) ColorfulTheme(darkSystem, content) else content()
}
