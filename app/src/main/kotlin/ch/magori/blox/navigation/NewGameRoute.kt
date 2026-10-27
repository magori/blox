package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.game.navigation.GameRoute
import kotlin.random.Random

/** The key of a new game at [startLevel], with a random seed: the home screen uses it to start a game (ADR 0011). */
internal fun newGameRoute(
    startLevel: Int = 0,
    random: Random = Random.Default,
): GameRoute = GameRoute(startLevel = startLevel, seed = random.nextLong())

/**
 * « Rejouer » after the [finished] game: the game its variant gives ([GameRoute.replayed]) takes its place on top of
 * [backStack], a Classic game with a new seed, a daily game with the same seed. A different key, so a new view model
 * (ADR 0011). Once [finished] is no longer on top (a second tap on Rejouer), nothing changes.
 */
internal fun replay(
    backStack: MutableList<NavKey>,
    finished: GameRoute,
    random: Random = Random.Default,
) {
    if (backStack.lastOrNull() != finished) return
    backStack[backStack.lastIndex] = finished.replayed(random)
}
