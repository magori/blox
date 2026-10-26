package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
}
