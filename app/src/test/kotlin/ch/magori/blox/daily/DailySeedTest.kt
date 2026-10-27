package ch.magori.blox.daily

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DailySeedTest {
    @Test
    fun `the seed of a day is its date written year month day`() {
        // When
        val seed = DailySeed.of(LocalDate.of(2026, 10, 26))
        val seedOfASingleDigitDay = DailySeed.of(LocalDate.of(2027, 1, 5))

        // Then the month and the day always take two digits
        assertThat(seed).isEqualTo(20261026L)
        assertThat(seedOfASingleDigitDay).isEqualTo(20270105L)
    }

    @Test
    fun `the date is the local date of the phone`() {
        // Given late on the 26th of October in London, already the 27th in Zurich
        val instant = Instant.parse("2026-10-26T23:30:00Z")
        val london = Clock.fixed(instant, ZoneId.of("Europe/London"))
        val zurich = Clock.fixed(instant, ZoneId.of("Europe/Zurich"))

        // When
        val inLondon = DailySeed.today(london)
        val inZurich = DailySeed.today(zurich)

        // Then
        assertThat(inLondon).isEqualTo(20261026L)
        assertThat(inZurich).isEqualTo(20261027L)
    }

    @Test
    fun `two days give two seeds`() {
        // When
        val monday = DailySeed.of(LocalDate.of(2026, 10, 26))
        val tuesday = DailySeed.of(LocalDate.of(2026, 10, 27))

        // Then
        assertThat(tuesday).isNotEqualTo(monday)
    }

    @Test
    fun `a later day gives a greater seed, from one year to the next`() {
        // When
        val newYearsEve = DailySeed.of(LocalDate.of(2026, 12, 31))
        val newYearsDay = DailySeed.of(LocalDate.of(2027, 1, 1))

        // Then
        assertThat(newYearsDay).isGreaterThan(newYearsEve)
    }
}
