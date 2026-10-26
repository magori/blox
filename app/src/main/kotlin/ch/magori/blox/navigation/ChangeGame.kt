package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute

/**
 * « Changer de jeu », on the home of a game: the cartridges (ADR 0022) come back, every screen above them leaving.
 * A stack restored without them, saved before they came, gets them at its root first, then loses what it held: it is
 * never empty, even for a moment.
 */
internal fun changeGame(backStack: MutableList<NavKey>) {
    if (CartridgesRoute !in backStack) backStack.add(0, CartridgesRoute)
    val cartridges = backStack.lastIndexOf(CartridgesRoute)
    backStack.subList(cartridges + 1, backStack.size).clear()
}
