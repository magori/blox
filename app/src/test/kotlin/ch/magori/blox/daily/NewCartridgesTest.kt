package ch.magori.blox.daily

import app.cash.turbine.test
import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.feature.cartridges.Cartridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class NewCartridgesTest {
    @Test
    fun `the Graine du jour is new before any try`() = runTest {
        // Given no results kept
        val results = flowOf<DailyResults?>(null)

        // When / Then
        assertThat(NewCartridges(results, backgroundScope) { TODAY }.updates.first()).containsExactly(Cartridge.DAILY)
    }

    @Test
    fun `the Graine du jour is no longer new once a try of today started`() = runTest {
        // Given a first try of today, under way: a try is counted as soon as it starts
        val results = flowOf<DailyResults?>(DailyResults(date = TODAY, tries = 1))

        // When / Then
        assertThat(NewCartridges(results, backgroundScope) { TODAY }.updates.first()).isEmpty()
    }

    @Test
    fun `the Graine du jour is new again the next day, the date read anew at each reading of the results`() = runTest {
        // Given the tries of yesterday, read again once the phone passed midnight
        var today = TODAY
        val results = MutableStateFlow<DailyResults?>(DailyResults(date = TODAY, tries = 2))

        NewCartridges(results, backgroundScope) { today }.updates.test {
            assertThat(awaitItem()).isEmpty()

            // When the results are read again on the next day, as when the cartridges show again
            today = TODAY.plusDays(1)
            results.value = DailyResults(date = TODAY, tries = 2, firstTry = 1_200)

            // Then
            assertThat(awaitItem()).containsExactly(Cartridge.DAILY)
        }
    }

    @Test
    fun `once the results are read, the cartridges new today are known at once, without waiting for a collection`() =
        runTest {
            // Given the results read once, kept warm
            val results = MutableStateFlow<DailyResults?>(null)
            var today = TODAY
            val newCartridges = NewCartridges(results, backgroundScope) { today }
            runCurrent()

            // When / Then: the date is read at each call
            assertThat(newCartridges.current()).containsExactly(Cartridge.DAILY)
            results.value = DailyResults(date = TODAY, tries = 1)
            runCurrent()
            assertThat(newCartridges.current()).isEmpty()
            today = TODAY.plusDays(1)
            assertThat(newCartridges.current()).containsExactly(Cartridge.DAILY)
        }

    @Test
    fun `before the results are read, no cartridge is new, so no sticker shows that may have to go`() = runTest {
        // Given results not read yet
        val newCartridges = NewCartridges(emptyFlow(), backgroundScope) { TODAY }
        runCurrent()

        // When / Then
        assertThat(newCartridges.current()).isEmpty()
    }
}

private val TODAY: LocalDate = LocalDate.of(2026, 10, 27)
