package ch.magori.blox.feature.locksmith

import app.cash.turbine.test
import ch.magori.blox.core.data.FakePadlockProgressRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.PadlockOpening
import ch.magori.blox.core.data.PadlockProgress
import ch.magori.blox.core.data.PadlockProgressRepository
import ch.magori.blox.core.locksmith.PadlockResult
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/** The screen « Serrures » (P7.35): what the view model reads of the progress of the Serrurier. */
@ExtendWith(MainDispatcherExtension::class)
class PadlocksViewModelTest {
    private val progress = FakePadlockProgressRepository()

    @Test
    fun `the screen shows the best opening of each open padlock, padlock 1 first`() = runTest {
        // Given padlocks 1 and 2 open
        progress.record(1, PERFECT_OF_3)
        progress.record(2, WITH_A_HINT_OF_4)

        // When / Then
        PadlocksViewModel(progress).uiState.test {
            assertThat(expectMostRecentItem()?.openings).containsExactly(
                PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 0),
                PadlockResult(pieces = 4, moves = 5, refusals = 1, hints = 1),
            )
        }
    }

    @Test
    fun `nothing is shown until the progress is read, so that no empty screen flashes`() = runTest {
        // Given a progress that is not read yet
        val unread = object : PadlockProgressRepository by progress {
            override val progress: Flow<PadlockProgress> = flow { awaitCancellation() }
        }

        // When / Then
        PadlocksViewModel(unread).uiState.test {
            assertThat(awaitItem()).isNull()
            expectNoEvents()
        }
    }

    @Test
    fun `the screen follows the progress, a padlock opened meanwhile shown`() = runTest {
        // Given the screen shown, nothing open
        PadlocksViewModel(progress).uiState.test {
            assertThat(expectMostRecentItem()?.openings).isEmpty()

            // When
            progress.record(1, PERFECT_OF_3)

            // Then
            assertThat(awaitItem()?.openings).containsExactly(PERFECT_OF_3.toResult())
        }
    }

    private companion object {
        val PERFECT_OF_3 = PadlockOpening(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val WITH_A_HINT_OF_4 = PadlockOpening(pieces = 4, moves = 5, refusals = 1, hints = 1)
    }
}
