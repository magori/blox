package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.settings.navigation.SettingsRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ChangeGameTest {
    @Test
    fun `changing the game from the home goes back to the cartridges`() {
        // Given the home of the Classic, opened from the cartridges, on the stack of the application
        val backStack = NavBackStack<NavKey>(CartridgesRoute, HomeRoute)

        // When
        changeGame(backStack)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute)
    }

    @Test
    fun `changing the game from a restored stack without cartridges puts them at its root`() {
        // Given the home alone, as a stack saved before the cartridges came (ADR 0022) is restored
        val backStack = NavBackStack<NavKey>(HomeRoute)

        // When
        changeGame(backStack)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute)
    }

    @Test
    fun `changing the game from a restored stack without cartridges never leaves the stack empty`() {
        // Given the home alone, restored, on a stack that records its size after each change
        val backStack = SizeRecordingList(HomeRoute)

        // When
        changeGame(backStack)

        // Then no screen is left without a key to show, even for a moment
        assertThat(backStack.sizes).doesNotContain(0)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stacksWithScreensAboveTheHome")
    fun `changing the game drops every screen above the home, down to the cartridges`(stack: List<NavKey>) {
        // Given
        val backStack = NavBackStack(*stack.toTypedArray())

        // When
        changeGame(backStack)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute)
    }

    @Test
    fun `changing the game once more, the cartridges shown, leaves them alone on the stack`() {
        // Given the game already changed: a late second tap
        val backStack = NavBackStack<NavKey>(CartridgesRoute)

        // When
        changeGame(backStack)

        // Then
        assertThat(backStack).containsExactly(CartridgesRoute)
    }

    /** A stack that records its size after each change, one key at a time. */
    private class SizeRecordingList(vararg keys: NavKey) : AbstractMutableList<NavKey>() {
        private val keys = keys.toMutableList()
        val sizes = mutableListOf<Int>()

        override val size: Int get() = keys.size

        override fun get(index: Int): NavKey = keys[index]

        override fun set(
            index: Int,
            element: NavKey,
        ): NavKey = keys.set(index, element).also { sizes += keys.size }

        override fun add(
            index: Int,
            element: NavKey,
        ) {
            keys.add(index, element)
            sizes += keys.size
        }

        override fun removeAt(index: Int): NavKey = keys.removeAt(index).also { sizes += keys.size }
    }

    private companion object {
        @JvmStatic
        fun stacksWithScreensAboveTheHome(): List<List<NavKey>> = listOf(
            // Restored without cartridges, the settings above the home
            listOf(HomeRoute, SettingsRoute()),
            // A game above the home
            listOf(CartridgesRoute, HomeRoute, GameRoute(startLevel = 0, seed = 1L)),
        )
    }
}
