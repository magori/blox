package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute

/**
 * « Changer de jeu », on the home of a game: the cartridges (ADR 0022) come back, every screen above them leaving.
 * A stack restored without them, saved before they came, gets them at its root in place of what it held.
 */
internal fun changeGame(backStack: MutableList<NavKey>) {
    val cartridges = backStack.lastIndexOf(CartridgesRoute)
    backStack.subList(cartridges + 1, backStack.size).clear()
    if (cartridges < 0) backStack.add(CartridgesRoute)
}
