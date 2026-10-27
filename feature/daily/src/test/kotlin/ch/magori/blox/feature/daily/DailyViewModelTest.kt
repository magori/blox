package ch.magori.blox.feature.daily

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.core.data.FakeDailyChallengeRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

class DailyViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val october26: LocalDate = LocalDate.of(2026, 10, 26)

    @Test
    fun `without a try today, no result is shown and the score cannot be copied`() = runTest {
        // Given no try at all
        val viewModel = dailyViewModel()

        viewModel.uiState.test {
            // Then
            val state = checkNotNull(awaitItem())
            assertThat(state.date).isEqualTo(october26)
            assertThat(state.tries).isZero()
            assertThat(state.firstTry).isNull()
            assertThat(state.best).isNull()
            assertThat(state.canCopy).isFalse()
        }
    }

    @Test
    fun `the results of today show the tries, the first try and the best, and the score can be copied`() = runTest {
        // Given three tries today
        val results = DailyResults(october26, tries = 3, firstTry = 9_800, best = 18_200, firstTryOver = true)
        val viewModel =
            dailyViewModel(results)

        viewModel.uiState.test {
            // Then
            val state = checkNotNull(awaitItem())
            assertThat(state.tries).isEqualTo(3)
            assertThat(state.firstTry).isEqualTo(9_800)
            assertThat(state.best).isEqualTo(18_200)
            assertThat(state.canCopy).isTrue()
        }
    }

    @Test
    fun `a try started but not ended shows a dash for the first try and the best`() = runTest {
        // Given a first try under way (or killed before its end): counted, worth 0 until it ends
        val results = DailyResults(october26, tries = 1)
        val viewModel =
            dailyViewModel(results)

        viewModel.uiState.test {
            // Then the try is counted, but neither its points nor a best are known yet
            val state = checkNotNull(awaitItem())
            assertThat(state.tries).isEqualTo(1)
            assertThat(state.firstTry).isNull()
            assertThat(state.best).isNull()
        }
    }

    @Test
    fun `a best of 0 is shown once the first try is over`() = runTest {
        // Given a first try ended at 0 points
        val results = DailyResults(october26, tries = 1, firstTry = 0, best = 0, firstTryOver = true)
        val viewModel =
            dailyViewModel(results)

        viewModel.uiState.test {
            // Then
            val state = checkNotNull(awaitItem())
            assertThat(state.firstTry).isZero()
            assertThat(state.best).isZero()
        }
    }

    @Test
    fun `the results of another day are not those of today`() = runTest {
        // Given tries yesterday, and tries tomorrow (the clock of the phone went back)
        val yesterday = DailyResults(october26.minusDays(1), tries = 2, firstTry = 500, best = 900, firstTryOver = true)
        val tomorrow = DailyResults(october26.plusDays(1), tries = 1, firstTry = 700, best = 700, firstTryOver = true)

        for (results in listOf(yesterday, tomorrow)) {
            val viewModel =
                dailyViewModel(results)

            viewModel.uiState.test {
                // Then no try today
                val state = checkNotNull(awaitItem())
                assertThat(state.tries).isZero()
                assertThat(state.firstTry).isNull()
                assertThat(state.best).isNull()
                assertThat(state.canCopy).isFalse()
            }
        }
    }

    @Test
    fun `the seven first are the first pieces of the daily game, drawn from the seed of the day`() = runTest {
        // Given the 26th of October 2026, seed 20261026
        val viewModel = dailyViewModel()

        viewModel.uiState.test {
            // Then the start of the pieces of that seed, a contract (ClassicRandomizerTest), which the daily game draws
            // too (GameViewModelTest, a daily game draws the pieces of its seed)
            val state = checkNotNull(awaitItem())
            assertThat(state.firstPieces.joinToString(" ")).isEqualTo("Z T I Z O Z J")
        }
    }

    @Test
    fun `the date is read again each time the screen shows, with the pieces and results of that day`() = runTest {
        // Given the screen shown late on the 26th, after a try that day
        var today = october26
        val results = DailyResults(october26, tries = 1, firstTry = 4_000, best = 4_000, firstTryOver = true)
        val viewModel = dailyViewModel(results, today = { today })

        viewModel.uiState.test {
            assertThat(checkNotNull(awaitItem()).date).isEqualTo(october26)

            // When the phone passes midnight, then the screen shows again
            today = october26.plusDays(1)
            viewModel.onShown()

            // Then the day after, its own pieces (the start of seed 20261027), and no try yet
            val state = checkNotNull(awaitItem())
            assertThat(state.date).isEqualTo(october26.plusDays(1))
            assertThat(state.firstPieces.joinToString(" ")).isEqualTo("L J T J O J Z")
            assertThat(state.tries).isZero()
            assertThat(state.best).isNull()
        }
    }

    @Test
    fun `the factory builds the view model on the results, the date and the seeds given by app`() = runTest {
        // Given the factory as app builds it
        val results = DailyResults(october26, tries = 2, firstTry = 300, best = 1_200, firstTryOver = true)
        val factory =
            DailyViewModel.Factory(FakeDailyChallengeRepository(results), startLevel = 5, today = {
                october26
            }, ::seedOf)

        // When
        val viewModel = ViewModelProvider.create(ViewModelStore(), factory)[DailyViewModel::class]

        // Then
        val state = checkNotNull(viewModel.uiState.value)
        assertThat(state.date).isEqualTo(october26)
        assertThat(state.firstPieces.joinToString(" ")).isEqualTo("Z T I Z O Z J")
        assertThat(state.best).isEqualTo(1_200)
    }

    @Test
    fun `the reminder under Jouer gives the start level of the daily game, as app gives it`() = runTest {
        // Given the start level app gives, the one of the daily game
        val viewModel = dailyViewModel(startLevel = 9)

        viewModel.uiState.test {
            // Then
            assertThat(checkNotNull(awaitItem()).startLevel).isEqualTo(9)
        }
    }

    /** The view model of the entry on [results] (none: no try), on the date [today] gives, the daily game at [startLevel]. */
    private fun dailyViewModel(
        results: DailyResults? = null,
        today: () -> LocalDate = { october26 },
        startLevel: Int = 5,
    ): DailyViewModel = DailyViewModel(FakeDailyChallengeRepository(results), startLevel, today, ::seedOf)

    /** The seed of [date], written year month day, as `app` gives it (DailySeed). */
    private fun seedOf(date: LocalDate): Long = date.year * 10_000L + date.monthValue * 100 + date.dayOfMonth
}
