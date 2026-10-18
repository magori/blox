package ch.magori.blox.navigation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class NewGameRouteTest {
    @Test
    fun `a new game route takes its seed from the random`() {
        // When
        val route = newGameRoute(random = Random(7))

        // Then
        assertThat(route.seed).isEqualTo(Random(7).nextLong())
    }

    @Test
    fun `a new game starts at level 0 by default`() {
        // When
        val route = newGameRoute(random = Random(7))

        // Then
        assertThat(route.startLevel).isZero()
    }
}
