package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.game.navigation.GameRoute
import kotlin.random.Random

/**
 * The key of a new game at [startLevel], with a random seed. The home screen uses it to start a
 * game, and [replay] to put a new key on the stack (ADR 0011).
 */
internal fun newGameRoute(
    startLevel: Int = 0,
    random: Random = Random.Default,
): GameRoute = GameRoute(startLevel = startLevel, seed = random.nextLong())

/**
 * « Rejouer » after the [finished] game: a new game at the same start level, with a new seed, takes its
 * place on top of [backStack]. A different key, so a new view model (ADR 0011). Once [finished] is no
 * longer on top (a second tap on Rejouer), nothing changes.
 */
internal fun replay(
    backStack: MutableList<NavKey>,
    finished: GameRoute,
    random: Random = Random.Default,
) {
    if (backStack.lastOrNull() != finished) return
    backStack[backStack.lastIndex] = newGameRoute(startLevel = finished.startLevel, random = random)
}
