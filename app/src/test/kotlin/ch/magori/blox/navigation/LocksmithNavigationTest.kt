package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class LocksmithNavigationTest {
    @Test
    fun `the Locksmith cartridge opens padlock 1 over the cartridges, on a seed drawn at random`() {
        // Given the choice of the game
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When
        openCartridge(backStack, Cartridge.LOCKSMITH, Random(42))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = 1, seed = Random(42).nextLong()))
    }

    @Test
    fun `the next padlock takes the place of the open one, one level up, on a new seed`() {
        // Given padlock 2, open
        val open = LocksmithRoute(level = 2, seed = 7)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, open)

        // When
        nextPadlock(backStack, open, Random(42))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = 3, seed = Random(42).nextLong()))
    }

    @Test
    fun `a second tap on next padlock does not move on twice`() {
        // Given padlock 2, already replaced by padlock 3 by a first tap
        val open = LocksmithRoute(level = 2, seed = 7)
        val third = LocksmithRoute(level = 3, seed = 9)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, third)

        // When the second tap arrives with the padlock it was on
        nextPadlock(backStack, open, Random(42))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, third)
    }
}
