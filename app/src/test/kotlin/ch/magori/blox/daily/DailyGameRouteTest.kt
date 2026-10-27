package ch.magori.blox.daily

import ch.magori.blox.feature.game.navigation.GameRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class DailyGameRouteTest {
    @Test
    fun `the date is the local date of the phone`() {
        // Given late on the 26th of October in London, already the 27th in Zurich
        val instant = Instant.parse("2026-10-26T23:30:00Z")
        val london = Clock.fixed(instant, ZoneId.of("Europe/London"))
        val zurich = Clock.fixed(instant, ZoneId.of("Europe/Zurich"))

        // When / Then each phone is on its local date
        assertThat(today(london)).isEqualTo(LocalDate.of(2026, 10, 26))
        assertThat(today(zurich)).isEqualTo(LocalDate.of(2026, 10, 27))
    }

    @Test
    fun `the daily game played is the game of the date shown, with its seed, even once the phone passed midnight`() {
        // Given the entry showing the 26th, Jouer tapped once the phone is on the 27th
        val shown = LocalDate.of(2026, 10, 26)

        // When
        val route = newDailyGameRoute(shown, random = Random(7))

        // Then the game of the pieces shown, not of the date of the phone
        assertThat(route).isEqualTo(GameRoute.daily(shown, seed = 20261026L, Random(7).nextLong()))
    }

    @Test
    fun `two daily games of the same day are two games, with two keys`() {
        // Given
        val date = LocalDate.of(2026, 10, 26)
        val random = Random(7)

        // When
        val first = newDailyGameRoute(date, random)
        val second = newDailyGameRoute(date, random)

        // Then the same seed, under two keys (ADR 0011)
        assertThat(second.seed).isEqualTo(first.seed)
        assertThat(second).isNotEqualTo(first)
    }

    @Test
    fun `the score copied at the game over is the one of the date of the game, even replayed after midnight`() {
        // Given a daily game of the 26th, replayed once the phone is on the 27th, and a game of the Classic
        val daily = newDailyGameRoute(LocalDate.of(2026, 10, 26), Random(7)).replayed(Random(8))
        val classic = GameRoute(startLevel = 0, seed = 1L)

        // When / Then
        assertThat(dailyDateOf(daily)).isEqualTo(LocalDate.of(2026, 10, 26))
        assertThat(dailyDateOf(classic)).isNull()
    }
}
