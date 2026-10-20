package ch.magori.blox.feature.composer.navigation

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ComposerRouteTest {
    @Test
    fun `the dice rolls on a new music only, an entry of a saved music that asks for it is refused`() {
        // When / Then
        assertThatThrownBy { ComposerRoute(musicKey = "joyeux", rollDice = true) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
