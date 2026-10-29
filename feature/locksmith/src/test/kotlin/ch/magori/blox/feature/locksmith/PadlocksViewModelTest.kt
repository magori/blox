package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import app.cash.turbine.test
import ch.magori.blox.core.data.FakePadlockProgressRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.PadlockOpening
import ch.magori.blox.core.data.PadlockProgress
import ch.magori.blox.core.data.PadlockProgressRepository
import ch.magori.blox.core.locksmith.PadlockResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

/**
 * The screen « Serrures » (P7.35): what the view model reads of the progress of the Serrurier; starting over from the
 * beginning (P7.37).
 */
@ExtendWith(MainDispatcherExtension::class)
class PadlocksViewModelTest {
    private val progress = FakePadlockProgressRepository()

    @Test
    fun `the screen shows the best opening of each open padlock, padlock 1 first`() = runTest {
        // Given padlocks 1 and 2 open
        progress.record(1, PERFECT_OF_3)
        progress.record(2, WITH_A_HINT_OF_4)

        // When / Then
        padlocksViewModel().uiState.test {
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
        padlocksViewModel(unread).uiState.test {
            assertThat(awaitItem()).isNull()
            expectNoEvents()
        }
    }

    @Test
    fun `the screen follows the progress, a padlock opened meanwhile shown`() = runTest {
        // Given the screen shown, nothing open
        padlocksViewModel().uiState.test {
            assertThat(expectMostRecentItem()?.openings).isEmpty()

            // When
            progress.record(1, PERFECT_OF_3)

            // Then
            assertThat(awaitItem()?.openings).containsExactly(PERFECT_OF_3.toResult())
        }
    }

    @Test
    fun `asking to start over from the beginning opens the confirmation, nothing cleared yet`() = runTest {
        // Given padlock 1 open, the screen shown
        progress.record(1, PERFECT_OF_3)
        val viewModel = padlocksViewModel()
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.NONE)

            // When
            viewModel.onStartOver()

            // Then
            val asked = awaitItem()
            assertThat(asked?.clearing).isEqualTo(ProgressClearing.ASKED)
            assertThat(asked?.openings).containsExactly(PERFECT_OF_3.toResult())
        }
    }

    @Test
    fun `keeping the progress closes the confirmation, every opening kept`() = runTest {
        // Given padlock 1 open, the confirmation open
        progress.record(1, PERFECT_OF_3)
        val viewModel = padlocksViewModel()
        viewModel.onStartOver()
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.ASKED)

            // When
            viewModel.onKeepProgress()

            // Then
            val kept = awaitItem()
            assertThat(kept?.clearing).isEqualTo(ProgressClearing.NONE)
            assertThat(kept?.openings).containsExactly(PERFECT_OF_3.toResult())
        }
    }

    @Test
    fun `clearing the progress, as the player confirmed, forgets every opening and hands the clear over`() = runTest {
        // Given padlocks 1 and 2 open, the confirmation open
        progress.record(1, PERFECT_OF_3)
        progress.record(2, WITH_A_HINT_OF_4)
        val cleared = ClearedProgressRelay()
        val viewModel = padlocksViewModel(cleared = cleared)
        viewModel.onStartOver()
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.ASKED)

            // When
            viewModel.onClearProgress()

            // Then
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.CLEARED)
            assertThat(progress.progress.first().openings).isEmpty()
            assertThat(cleared.take()).isTrue()
        }
    }

    @Test
    fun `once the clear is asked, the screen keeps the padlocks it showed until it is left, so that no 0 is told`() =
        runTest {
            // Given padlocks 1 and 2 open, the confirmation open
            progress.record(1, PERFECT_OF_3)
            progress.record(2, WITH_A_HINT_OF_4)
            val viewModel = padlocksViewModel()
            viewModel.onStartOver()
            viewModel.uiState.test {
                assertThat(expectMostRecentItem()?.open).isEqualTo(2)

                // When the progress is cleared
                viewModel.onClearProgress()

                // Then the window, open until the navigation, still tells the 2 padlocks forgotten
                val done = expectMostRecentItem()
                assertThat(done?.clearing).isEqualTo(ProgressClearing.CLEARED)
                assertThat(done?.open).isEqualTo(2)
            }
        }

    @Test
    fun `a progress that cannot be cleared keeps every opening, the confirmation saying it failed`() = runTest {
        // Given padlock 1 open, the confirmation open, a disk full
        progress.record(1, PERFECT_OF_3)
        progress.failure = IOException("Disk full")
        val cleared = ClearedProgressRelay()
        val viewModel = padlocksViewModel(cleared = cleared)
        viewModel.onStartOver()
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.ASKED)

            // When
            viewModel.onClearProgress()

            // Then
            val failed = expectMostRecentItem()
            assertThat(failed?.clearing).isEqualTo(ProgressClearing.FAILED)
            assertThat(failed?.openings).containsExactly(PERFECT_OF_3.toResult())
            assertThat(cleared.take()).isFalse()
        }
    }

    @Test
    fun `after a failure the player may confirm again, and the progress is cleared once it can be written`() = runTest {
        // Given padlock 1 open, a clear that failed on a full disk
        progress.record(1, PERFECT_OF_3)
        progress.failure = IOException("Disk full")
        val viewModel = padlocksViewModel()
        viewModel.onStartOver()
        viewModel.uiState.test {
            viewModel.onClearProgress()
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.FAILED)

            // When the disk has room again, and the player confirms again
            progress.failure = null
            viewModel.onClearProgress()

            // Then
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.CLEARED)
            assertThat(progress.progress.first().openings).isEmpty()
        }
    }

    @Test
    fun `a confirmation that comes when nothing is asked clears nothing`() = runTest {
        // Given padlock 1 open, the confirmation not open
        progress.record(1, PERFECT_OF_3)
        val viewModel = padlocksViewModel()
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.NONE)

            // When a late tap confirms
            viewModel.onClearProgress()

            // Then
            expectNoEvents()
        }
        assertThat(progress.progress.first().openings).hasSize(1)
    }

    @Test
    fun `while the clear is written the confirmation waits, and a second confirmation clears nothing more`() = runTest {
        // Given padlock 1 open, a disk slow to write the clear, the player having confirmed it
        progress.record(1, PERFECT_OF_3)
        val written = CompletableDeferred<Unit>()
        var clears = 0
        val slow = object : PadlockProgressRepository by progress {
            override suspend fun clear() {
                clears++
                written.await()
                this@PadlocksViewModelTest.progress.clear()
            }
        }
        val viewModel = padlocksViewModel(progress = slow)
        viewModel.onStartOver()
        viewModel.uiState.test {
            viewModel.onClearProgress()
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.CLEARING)

            // When the player confirms again, then keeps the progress, before the clear is written
            viewModel.onClearProgress()
            viewModel.onKeepProgress()
            viewModel.onStartOver()

            // Then the confirmation still waits, the clear asked once
            expectNoEvents()
            assertThat(clears).isEqualTo(1)
            written.complete(Unit)
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.CLEARED)
            assertThat(clears).isEqualTo(1)
        }
    }

    @Test
    fun `once the progress is cleared, a late answer changes nothing until the screen goes`() = runTest {
        // Given padlock 1 open, its clear done
        progress.record(1, PERFECT_OF_3)
        val viewModel = padlocksViewModel()
        viewModel.onStartOver()
        viewModel.uiState.test {
            viewModel.onClearProgress()
            assertThat(expectMostRecentItem()?.clearing).isEqualTo(ProgressClearing.CLEARED)

            // When a late tap keeps the progress, then asks again
            viewModel.onKeepProgress()
            viewModel.onStartOver()

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `opening the screen of the padlocks forgets a clear that no padlock took`() = runTest {
        // Given a clear handed over, no padlock shown since to take it
        val cleared = ClearedProgressRelay()
        cleared.handOver()

        // When the screen of the padlocks opens
        padlocksViewModel(cleared = cleared)

        // Then no padlock shown later starts over for it
        assertThat(cleared.take()).isFalse()
    }

    @Test
    fun `the clear outlives the screen, written and handed over even once the screen is left`() = runTest {
        // Given padlock 1 open, a disk slow to write the clear, in the scope of the application
        progress.record(1, PERFECT_OF_3)
        val slow = object : PadlockProgressRepository by progress {
            override suspend fun clear() {
                delay(WRITE_MILLIS)
                this@PadlocksViewModelTest.progress.clear()
            }
        }
        val cleared = ClearedProgressRelay()
        val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(
            store,
            PadlocksViewModel.Factory(slow, cleared, appScope),
            CreationExtras.Empty,
        )[PadlocksViewModel::class]

        // When the player confirms, and the screen is left at once
        viewModel.onStartOver()
        viewModel.onClearProgress()
        store.clear()
        advanceUntilIdle()

        // Then
        assertThat(progress.progress.first().openings).isEmpty()
        assertThat(cleared.take()).isTrue()
    }

    /** The view model of the screen, on [progress], handing a clear over to [cleared], written in [appScope]. */
    private fun padlocksViewModel(
        progress: PadlockProgressRepository = this@PadlocksViewModelTest.progress,
        cleared: ClearedProgressRelay = ClearedProgressRelay(),
        appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    ) = PadlocksViewModel(progress, cleared, appScope)

    private companion object {
        val PERFECT_OF_3 = PadlockOpening(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val WITH_A_HINT_OF_4 = PadlockOpening(pieces = 4, moves = 5, refusals = 1, hints = 1)

        /** How long a slow disk takes to write the clear. */
        const val WRITE_MILLIS = 1_000L
    }
}
