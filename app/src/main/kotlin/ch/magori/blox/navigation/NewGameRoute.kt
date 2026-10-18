package ch.magori.blox.navigation

import ch.magori.blox.feature.game.navigation.GameRoute
import kotlin.random.Random

/**
 * The key of a new game at [startLevel], with a random seed. The home screen uses it to start a
 * game, and « Rejouer » will use it to put a new key on the stack (ADR 0011).
 */
internal fun newGameRoute(
    startLevel: Int = 0,
    random: Random = Random.Default,
): GameRoute = GameRoute(startLevel = startLevel, seed = random.nextLong())
