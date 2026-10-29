package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.daily.navigation.DailyRoute
import ch.magori.blox.feature.garden.navigation.GardenRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute

/**
 * The [cartridge] tapped on the choice of the game opens the home of its game over [backStack] (ADR 0022): the home of
 * the Classic, padlock 1 of the Locksmith (ADR 0025), the musical garden, which reads the garden kept itself (ADR 0027),
 * or the entry of the Graine du jour (ADR 0026). Only from the cartridges on top: once a game is on its way, a second
 * tap stacks nothing. Each cartridge names its home here, the only place that knows the features; a new cartridge does
 * not compile until it is given one.
 */
internal fun openCartridge(
    backStack: MutableList<NavKey>,
    cartridge: Cartridge,
) {
    if (backStack.lastOrNull() != CartridgesRoute) return
    when (cartridge) {
        Cartridge.CLASSIC -> backStack.add(HomeRoute)
        Cartridge.LOCKSMITH -> backStack.add(LocksmithRoute(level = 1))
        Cartridge.GARDEN -> backStack.add(GardenRoute)
        Cartridge.DAILY -> backStack.add(DailyRoute)
    }
}
