package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import kotlin.random.Random

/**
 * The [cartridge] tapped on the choice of the game opens the home of its game over [backStack] (ADR 0022): the home of
 * the Classic, or padlock 1 of the Locksmith on a seed drawn from [random] (ADR 0025). Only from the cartridges on
 * top: once a game is on its way, a second tap stacks nothing. Each cartridge names its home here, the only place that
 * knows both features; a new cartridge does not compile until it is given one.
 */
internal fun openCartridge(
    backStack: MutableList<NavKey>,
    cartridge: Cartridge,
    random: Random = Random.Default,
) {
    if (backStack.lastOrNull() != CartridgesRoute) return
    when (cartridge) {
        Cartridge.CLASSIC -> backStack.add(HomeRoute)
        Cartridge.LOCKSMITH -> backStack.add(LocksmithRoute(level = 1, seed = random.nextLong()))
    }
}
