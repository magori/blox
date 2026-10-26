package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChangeGameTest {
    @Test
    fun `changing the game from the home goes back to the cartridges`() {
        // Given the home of the Classic, opened from the cartridges
        val backStack = mutableListOf<NavKey>(CartridgesRoute, HomeRoute)

        // When
        changeGame(backStack)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute)
    }

    @Test
    fun `changing the game from a restored stack without cartridges puts them at its root`() {
        // Given the home alone, as a stack saved before the cartridges came (ADR 0022) is restored
        val backStack = mutableListOf<NavKey>(HomeRoute)

        // When
        changeGame(backStack)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute)
    }

    @Test
    fun `changing the game once more, the cartridges shown, leaves them alone on the stack`() {
        // Given the game already changed: a late second tap
        val backStack = mutableListOf<NavKey>(CartridgesRoute)

        // When
        changeGame(backStack)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute)
    }
}
