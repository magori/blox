package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.daily.navigation.DailyRoute
import ch.magori.blox.feature.garden.navigation.GardenRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenCartridgeTest {
    @Test
    fun `the Classic cartridge opens the Classic home over the cartridges, at once`() = runTest {
        // Given the choice of the game
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When, nothing else run
        opener(backStack).open(Cartridge.CLASSIC)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, HomeRoute)
    }

    @Test
    fun `a second tap does not stack a second home`() = runTest {
        // Given the Classic home, already opened by a first tap
        val backStack = NavBackStack<NavKey>(CartridgesRoute, HomeRoute)

        // When the second tap of a double tap arrives
        opener(backStack).open(Cartridge.CLASSIC)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, HomeRoute)
    }

    @Test
    fun `the Locksmith cartridge opens the frontier, the first padlock not open, over the cartridges`() = runTest {
        // Given the choice of the game, padlocks 1 to 12 open
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When
        opener(backStack).open(Cartridge.LOCKSMITH)
        runCurrent()

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = FRONTIER))
    }

    @Test
    fun `a second tap on the Locksmith cartridge does not stack a second padlock`() = runTest {
        // Given the frontier, already opened by a first tap
        val first = LocksmithRoute(level = FRONTIER)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, first)

        // When the second tap of a double tap arrives
        opener(backStack).open(Cartridge.LOCKSMITH)
        runCurrent()

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, first)
    }

    @Test
    fun `two taps on the Locksmith cartridge while the frontier is read stack a single padlock`() = runTest {
        // Given the choice of the game, the progress still being read
        val backStack = NavBackStack<NavKey>(CartridgesRoute)
        val progressRead = CompletableDeferred<Int>()
        val opener = opener(backStack) { progressRead.await() }

        // When both taps of a double tap arrive before the reading, which then gives the frontier
        repeat(2) {
            opener.open(Cartridge.LOCKSMITH)
            runCurrent()
        }
        progressRead.complete(FRONTIER)
        runCurrent()

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, LocksmithRoute(level = FRONTIER))
    }

    @Test
    fun `a tap on the Locksmith left waiting while a game was opened and left opens no padlock once read`() = runTest {
        // Given a tap on the Locksmith cartridge, the progress still being read
        val backStack = NavBackStack<NavKey>(CartridgesRoute)
        val progressRead = CompletableDeferred<Int>()
        val opener = opener(backStack) { progressRead.await() }
        opener.open(Cartridge.LOCKSMITH)
        runCurrent()

        // When the Classic is opened, then left for the cartridges, before the reading gives the frontier
        opener.open(Cartridge.CLASSIC)
        changeGame(backStack)
        progressRead.complete(FRONTIER)
        runCurrent()

        // Then the cartridges show, as the player left them
        assertThat(backStack).containsExactly(CartridgesRoute)
    }

    @Test
    fun `a game opened while the frontier of the Locksmith is read is not covered by a padlock`() = runTest {
        // Given a tap on the Locksmith cartridge, the progress still being read
        val backStack = NavBackStack<NavKey>(CartridgesRoute)
        val progressRead = CompletableDeferred<Int>()
        val opener = opener(backStack) { progressRead.await() }
        opener.open(Cartridge.LOCKSMITH)
        runCurrent()

        // When the Classic cartridge is tapped before the reading, which then gives the frontier
        opener.open(Cartridge.CLASSIC)
        progressRead.complete(FRONTIER)
        runCurrent()

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, HomeRoute)
    }

    @Test
    fun `the Graine du jour cartridge opens its entry over the cartridges`() = runTest {
        // Given the choice of the game
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When, nothing else run
        opener(backStack).open(Cartridge.DAILY)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, DailyRoute)
    }

    @Test
    fun `the Jardin musical cartridge opens the musical garden over the cartridges`() = runTest {
        // Given the choice of the game
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When, nothing else run
        opener(backStack).open(Cartridge.GARDEN)

        // Then: the screen of the garden reads the garden kept itself
        assertThat(backStack).containsExactly(CartridgesRoute, GardenRoute)
    }

    /** The cartridges over [backStack], the tap on the Locksmith waiting in this test for [frontier]. */
    private fun TestScope.opener(
        backStack: NavBackStack<NavKey>,
        frontier: suspend () -> Int = { FRONTIER },
    ): CartridgeOpener = CartridgeOpener(backStack, backgroundScope, frontier)
}

/** The frontier of the Serrurier, padlocks 1 to 12 open. */
private const val FRONTIER = 13
