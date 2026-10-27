package ch.magori.blox.feature.daily

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class DayInLettersTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "2026-10-26, 26 octobre",
        "2027-01-05, 5 janvier",
        "2026-08-15, 15 août",
        "2026-12-31, 31 décembre",
    )
    fun `the day is written in letters, the day of the month then the month`(
        date: LocalDate,
        expected: String,
    ) {
        assertThat(dayInLetters(date)).isEqualTo(expected)
    }

    @Test
    fun `the first of the month is written 1er, as in French`() {
        assertThat(dayInLetters(LocalDate.of(2026, 11, 1))).isEqualTo("1er novembre")
    }
}
