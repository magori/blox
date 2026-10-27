package ch.magori.blox.daily

import android.os.Build
import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.core.data.FakeDailyChallengeRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DailyScoreCopierTest {
    private val daily = FakeDailyChallengeRepository()
    private val copied = mutableListOf<String>()
    private var confirmations = 0

    private fun TestScope.copier(sdkInt: Int = ANDROID_13): DailyScoreCopier = DailyScoreCopier(
        results = daily.results,
        texts = appScoreTexts,
        clipboard = { text -> copied += text },
        confirm = { confirmations++ },
        sdkInt = sdkInt,
        scope = backgroundScope,
    )

    @Test
    fun `copying puts the text of the results of the date in the clipboard`() = runTest {
        // Given five tries on the 26th of October
        daily.state.value = DailyResults(OCTOBER_26, tries = 5, firstTry = 450, best = 980, firstTryOver = true)

        // When the score of the 26th is copied
        copier().copy(OCTOBER_26)
        runCurrent()

        // Then
        assertThat(copied).containsExactly("BloX 20261026 · 1er 450 · meilleur 980 (5 essais)")
    }

    @Test
    fun `the results of another date are never copied under the date asked for`() = runTest {
        // Given results already of the 27th, the game of the 26th replayed after midnight not counted
        val october27 = OCTOBER_26.plusDays(1)
        daily.state.value = DailyResults(october27, tries = 1, firstTry = 300, best = 300, firstTryOver = true)

        // When the score of the 26th is copied
        copier().copy(OCTOBER_26)
        runCurrent()

        // Then nothing is copied, nor said to be
        assertThat(copied).isEmpty()
        assertThat(confirmations).isZero()
    }

    @Test
    fun `below Android 13 a message says the score is copied`() = runTest {
        // Given a try of the 26th, on Android 12L, which says nothing of a copy
        daily.state.value = DailyResults(OCTOBER_26, tries = 1, firstTry = 300, best = 300, firstTryOver = true)

        // When
        copier(sdkInt = ANDROID_13 - 1).copy(OCTOBER_26)
        runCurrent()

        // Then
        assertThat(copied).hasSize(1)
        assertThat(confirmations).isEqualTo(1)
    }

    @Test
    fun `from Android 13 the system says it, the app adds no message`() = runTest {
        // Given a try of the 26th, on Android 13, which shows what was copied
        daily.state.value = DailyResults(OCTOBER_26, tries = 1, firstTry = 300, best = 300, firstTryOver = true)

        // When
        copier(sdkInt = ANDROID_13).copy(OCTOBER_26)
        runCurrent()

        // Then
        assertThat(copied).hasSize(1)
        assertThat(confirmations).isZero()
    }

    @Test
    fun `without results nothing is copied`() = runTest {
        // Given no try kept, or results that could not be read
        daily.state.value = null

        // When
        copier().copy(OCTOBER_26)
        runCurrent()

        // Then
        assertThat(copied).isEmpty()
        assertThat(confirmations).isZero()
    }

    @Test
    fun `the copy reads the results when it is asked for, the try that just ended included`() = runTest {
        // Given a copier made before a second try of the 26th ends with a new best
        val copier = copier()
        daily.state.value = DailyResults(OCTOBER_26, tries = 1, firstTry = 300, best = 300, firstTryOver = true)
        daily.startTry(OCTOBER_26)
        daily.endTry(OCTOBER_26, points = 1_200)

        // When
        copier.copy(OCTOBER_26)
        runCurrent()

        // Then
        assertThat(copied).containsExactly("BloX 20261026 · 1er 300 · meilleur 1\u202F200 (2 essais)")
    }

    private companion object {
        val OCTOBER_26: LocalDate = LocalDate.of(2026, 10, 26)

        const val ANDROID_13 = Build.VERSION_CODES.TIRAMISU
    }
}
