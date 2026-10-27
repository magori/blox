package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.daily.navigation.DailyRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OpenCartridgeTest {
    @Test
    fun `the Classic cartridge opens the Classic home over the cartridges`() {
        // Given the choice of the game
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When
        openCartridge(backStack, Cartridge.CLASSIC)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, HomeRoute)
    }

    @Test
    fun `a second tap does not stack a second home`() {
        // Given the Classic home, already opened by a first tap
        val backStack = NavBackStack<NavKey>(CartridgesRoute, HomeRoute)

        // When the second tap of a double tap arrives
        openCartridge(backStack, Cartridge.CLASSIC)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, HomeRoute)
    }

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
    fun `a second tap on the Locksmith cartridge does not stack a second padlock`() {
        // Given padlock 1, already opened by a first tap
        val first = LocksmithRoute(level = 1, seed = 7)
        val backStack = NavBackStack<NavKey>(CartridgesRoute, first)

        // When the second tap of a double tap arrives
        openCartridge(backStack, Cartridge.LOCKSMITH, Random(42))

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, first)
    }

    @Test
    fun `the Graine du jour cartridge opens its entry over the cartridges`() {
        // Given the choice of the game
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When
        openCartridge(backStack, Cartridge.DAILY)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute, DailyRoute)
    }
}
