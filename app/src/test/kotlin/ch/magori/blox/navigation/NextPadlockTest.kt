package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NextPadlockTest {
    @Test
    fun `the next padlock takes the place of the open one, one level up`() {
        // Given padlock 2, open
        val open = LocksmithRoute(level = 2)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, open)

        // When
        nextPadlock(backStack, open)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = 3))
    }

    @Test
    fun `a second tap on next padlock does not move on twice`() {
        // Given padlock 2, already replaced by padlock 3 by a first tap
        val open = LocksmithRoute(level = 2)
        val third = LocksmithRoute(level = 3)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, third)

        // When the second tap arrives with the padlock it was on
        nextPadlock(backStack, open)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, third)
    }

    @Test
    fun `resuming puts the frontier in the place of the padlock replayed`() {
        // Given padlock 2, replayed, while the frontier is padlock 9
        val open = LocksmithRoute(level = 2)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, open)

        // When
        resumePadlocks(backStack, open, frontier = LocksmithRoute(level = 9))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = 9))
    }

    @Test
    fun `a second tap on resume does not move on twice`() {
        // Given padlock 2, already replaced by the frontier by a first tap
        val open = LocksmithRoute(level = 2)
        val frontier = LocksmithRoute(level = 9)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, frontier)

        // When the second tap arrives with the padlock it was on
        resumePadlocks(backStack, open, frontier)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, frontier)
    }
}
