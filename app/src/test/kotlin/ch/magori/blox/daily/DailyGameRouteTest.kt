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

        // When
        val inLondon = newDailyGameRoute(london, random = Random(7))
        val inZurich = newDailyGameRoute(zurich, random = Random(7))

        // Then each game is the game of its local date, with the seed of that date
        val id = Random(7).nextLong()
        assertThat(inLondon).isEqualTo(GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id))
        assertThat(inZurich).isEqualTo(GameRoute.daily(LocalDate.of(2026, 10, 27), seed = 20261027L, id))
    }

    @Test
    fun `two daily games of the same day are two games, with two keys`() {
        // Given
        val clock = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("Europe/Zurich"))
        val random = Random(7)

        // When
        val first = newDailyGameRoute(clock, random)
        val second = newDailyGameRoute(clock, random)

        // Then the same seed, under two keys (ADR 0011)
        assertThat(second.seed).isEqualTo(first.seed)
        assertThat(second).isNotEqualTo(first)
    }
}
