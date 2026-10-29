package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import ch.magori.blox.feature.locksmith.navigation.PadlocksRoute
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

    @Test
    fun `the disc of a padlock opens the screen of the padlocks over it, that padlock current`() {
        // Given padlock 2, open
        val open = LocksmithRoute(level = 2)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, open)

        // When
        openPadlocks(backStack, open)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, open, PadlocksRoute(current = 2))
    }

    @Test
    fun `a second tap on the disc does not open the screen of the padlocks twice`() {
        // Given the screen of the padlocks, opened over padlock 2 by a first tap
        val open = LocksmithRoute(level = 2)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, open, PadlocksRoute(current = 2))

        // When the second tap arrives with the padlock it was on
        openPadlocks(backStack, open)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, open, PadlocksRoute(current = 2))
    }

    @Test
    fun `a padlock chosen on the screen of the padlocks takes the place of the padlock under it, and the list goes`() {
        // Given the screen of the padlocks over padlock 9
        val list = PadlocksRoute(current = 9)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, LocksmithRoute(level = 9), list)

        // When padlock 4 is chosen there
        choosePadlock(backStack, list, LocksmithRoute(level = 4))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = 4))
    }

    @Test
    fun `choosing the padlock one comes from only goes back to it, the same key kept`() {
        // Given the screen of the padlocks over padlock 9, on a plain list: what keeps the key is choosePadlock itself,
        // whatever the stack does with an equal element
        val open = LocksmithRoute(level = 9)
        val list = PadlocksRoute(current = 9)
        val backStack = mutableListOf<NavKey>(CartridgesRoute, open, list)

        // When padlock 9 is chosen there, by a key equal to the padlock under the screen, but another instance, as the
        // screen of the padlocks makes it
        choosePadlock(backStack, list, LocksmithRoute(level = 9))

        // Then the padlock is the one under the screen, not a new one: its view model, and its pieces placed, stay
        assertThat(backStack).containsExactly(CartridgesRoute, open)
        assertThat(backStack.last()).isSameAs(open)
    }

    @Test
    fun `a second tap on the screen of the padlocks moves nothing more`() {
        // Given padlock 4, chosen by a first tap on the screen of the padlocks opened over padlock 9
        val list = PadlocksRoute(current = 9)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, LocksmithRoute(level = 4))

        // When the second tap arrives, on padlock 5, with the screen it was on
        choosePadlock(backStack, list, LocksmithRoute(level = 5))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = 4))
    }

    @Test
    fun `with no padlock under the screen of the padlocks, the padlock chosen takes its place`() {
        // Given the screen of the padlocks right over the cartridges, from no padlock
        val list = PadlocksRoute()
        val backStack = NavBackStack<NavKey>(CartridgesRoute, list)

        // When padlock 4 is chosen there
        choosePadlock(backStack, list, LocksmithRoute(level = 4))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = 4))
    }
}
