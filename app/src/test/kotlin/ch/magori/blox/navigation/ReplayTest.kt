package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

class ReplayTest {
    @Test
    fun `replaying puts a new game at the same start level in place of the finished one`() {
        // Given a game started at level 7, over, on top of the home screen
        val finished = GameRoute(startLevel = 7, seed = 1L)
        val backStack = mutableListOf<NavKey>(HomeRoute, finished)

        // When
        replay(backStack, finished, random = Random(7))

        // Then the new game has a new seed, so a new view model (ADR 0011)
        assertThat(backStack).containsExactly(HomeRoute, GameRoute(startLevel = 7, seed = Random(7).nextLong()))
    }

    @Test
    fun `a second replay of the same finished game changes nothing`() {
        // Given a finished game already replayed once
        val finished = GameRoute(startLevel = 7, seed = 1L)
        val backStack = mutableListOf<NavKey>(HomeRoute, finished)
        val random = Random(7)
        replay(backStack, finished, random)
        val afterFirst = backStack.toList()

        // When Rejouer is tapped a second time, for the same finished game
        replay(backStack, finished, random)

        // Then the new game stays, the only one
        assertThat(backStack).isEqualTo(afterFirst)
    }

    @Test
    fun `replaying a daily game keeps its seed, even after midnight`() {
        // Given the daily game of the 26th of October, over, replayed once the phone is on the 27th: the route holds its
        // date, which nothing reads again
        val finished = GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id = 1L)
        val backStack = mutableListOf<NavKey>(HomeRoute, finished)

        // When
        replay(backStack, finished, random = Random(7))

        // Then the new game is a daily game of the same date and seed, under a new key (ADR 0011)
        val replayed = backStack.last()
        assertThat(replayed).isEqualTo(
            GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id = Random(7).nextLong()),
        )
        assertThat(replayed).isNotEqualTo(finished)
    }
}
