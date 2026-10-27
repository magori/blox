package ch.magori.blox.daily

import ch.magori.blox.core.data.DailyResults
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DailyScoreTextTest {
    @Test
    fun `the copied text gives the date, the first try, the best and the tries`() {
        // Given five tries on the 26th of October 2026, the first worth 450, the best 980
        val results = DailyResults(OCTOBER_26, tries = 5, firstTry = 450, best = 980, firstTryOver = true)

        // When
        val text = dailyScoreText(results, appScoreTexts)

        // Then
        assertThat(text).isEqualTo("BloX 20261026 · 1er 450 · meilleur 980 (5 essais)")
    }

    @Test
    fun `one try is written in the singular`() {
        // Given a single try, over
        val results = DailyResults(OCTOBER_26, tries = 1, firstTry = 450, best = 450, firstTryOver = true)

        // When
        val text = dailyScoreText(results, appScoreTexts)

        // Then
        assertThat(text).endsWith("(1 essai)")
    }

    @Test
    fun `numbers are grouped by three`() {
        // Given a first try worth 9 800, a best worth 18 200
        val results = DailyResults(OCTOBER_26, tries = 5, firstTry = 9_800, best = 18_200, firstTryOver = true)

        // When
        val text = dailyScoreText(results, appScoreTexts)

        // Then each group of three digits is set apart by a narrow no-break space (U+202F)
        assertThat(text).isEqualTo("BloX 20261026 · 1er 9\u202F800 · meilleur 18\u202F200 (5 essais)")
    }

    @Test
    fun `numbers are written whole, beyond the six digits the screen shows, without leading zeros`() {
        // Given a best beyond 999 999, which the screen caps
        val results = DailyResults(OCTOBER_26, tries = 2, firstTry = 42, best = 1_234_567, firstTryOver = true)

        // When
        val text = dailyScoreText(results, appScoreTexts)

        // Then
        assertThat(text).isEqualTo("BloX 20261026 · 1er 42 · meilleur 1\u202F234\u202F567 (2 essais)")
    }

    @Test
    fun `a try worth nothing is written 0`() {
        // Given a first try over without a point
        val results = DailyResults(OCTOBER_26, tries = 1, firstTry = 0, best = 0, firstTryOver = true)

        // When
        val text = dailyScoreText(results, appScoreTexts)

        // Then
        assertThat(text).isEqualTo("BloX 20261026 · 1er 0 · meilleur 0 (1 essai)")
    }

    @Test
    fun `a first try not over is written with a dash, as the entry shows it`() {
        // Given a first try whose end was never written: it is worth 0 for now, not 0 points
        val results = DailyResults(OCTOBER_26, tries = 1, firstTry = 0, best = 0, firstTryOver = false)

        // When
        val text = dailyScoreText(results, appScoreTexts)

        // Then
        assertThat(text).isEqualTo("BloX 20261026 · 1er — · meilleur — (1 essai)")
    }

    @Test
    fun `a best given by a later try shows beside a first try not over`() {
        // Given a first try never ended, then a second over with 700 points
        val results = DailyResults(OCTOBER_26, tries = 2, firstTry = 0, best = 700, firstTryOver = false)

        // When
        val text = dailyScoreText(results, appScoreTexts)

        // Then
        assertThat(text).isEqualTo("BloX 20261026 · 1er — · meilleur 700 (2 essais)")
    }

    private companion object {
        val OCTOBER_26: LocalDate = LocalDate.of(2026, 10, 26)
    }
}
