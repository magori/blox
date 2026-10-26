package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.home.navigation.HomeRoute

/**
 * The [cartridge] tapped on the choice of the game opens the home of its game over [backStack] (ADR 0022). Only from
 * the cartridges on top: once a home is on its way, a second tap stacks nothing. Each cartridge names its home here,
 * the only place that knows both features; a new cartridge does not compile until it is given one.
 */
internal fun openCartridge(
    backStack: MutableList<NavKey>,
    cartridge: Cartridge,
) {
    if (backStack.lastOrNull() != CartridgesRoute) return
    when (cartridge) {
        Cartridge.CLASSIC -> backStack.add(HomeRoute)
    }
}
