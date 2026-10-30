package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.MutableCreationExtras
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakePadlockProgressRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.OpeningOutcome
import ch.magori.blox.core.data.PadlockOpening
import ch.magori.blox.core.data.PadlockProgress
import ch.magori.blox.core.data.PadlockProgressRepository
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

/** The opening of a padlock written to the progress of the Serrurier, and what the card tells of it (P7.33). */
@ExtendWith(MainDispatcherExtension::class)
class LocksmithProgressTest {
    private val padlocks = FakePadlockProgressRepository()

    /** What the scope of the application lets through, as Android would crash on it. */
    private val uncaught = mutableListOf<Throwable>()

    /** The scope of the application, where the opening is written, running at once. */
    private val appScope = CoroutineScope(
        SupervisorJob() + UnconfinedTestDispatcher() + CoroutineExceptionHandler { _, failure -> uncaught += failure },
    )

    @Test
    fun `opening the padlock records its opening, for its level`() = runTest {
        // Given padlock 1, the frontier
        val viewModel = viewModel(level = 1)

        // When
        viewModel.opening()

        // Then
        val result = viewModel.uiState.value.game.result!!
        assertThat(padlocks.progress.first().openings).containsExactly(result.toOpening())
    }

    @Test
    fun `a padlock beyond the frontier is not recorded, and the screen tells it`() = runTest {
        // Given padlock 2, while padlock 1 is not open, as after an opening that could not be written
        val viewModel = viewModel(level = 2)

        // When
        viewModel.opening()

        // Then
        assertThat(uncaught).isEmpty()
        assertThat(padlocks.progress.first()).isEqualTo(PadlockProgress())
        assertThat(viewModel.uiState.value.unrecorded).isTrue
    }

    @Test
    fun `the opening is recorded even when the screen is left at once`() = runTest {
        // Given padlock 1 in a view model kept as the navigation entry keeps it, its progress slow to write
        val slow = object : PadlockProgressRepository by padlocks {
            override suspend fun record(
                padlock: Int,
                opening: PadlockOpening,
            ): OpeningOutcome {
                delay(WRITE_MILLIS)
                return padlocks.record(padlock, opening)
            }
        }
        val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val extras = MutableCreationExtras().apply { set(LocksmithViewModel.RouteKey, LocksmithRoute(1)) }
        val factory = LocksmithViewModel.Factory(
            FakeSoundEffectPlayer(),
            FakeUserPreferencesRepository(),
            slow,
            appScope,
            ClearedProgressRelay(),
        )
        val viewModel = ViewModelProvider.create(store, factory, extras)[LocksmithViewModel::class]

        // When the padlock opens, and the entry leaves the stack at once (back, or the next padlock)
        viewModel.opening()
        store.clear()
        advanceUntilIdle()

        // Then
        assertThat(padlocks.progress.first().openings).hasSize(1)
    }

    @Test
    fun `an opening that cannot be written leaves the game playable, and the screen tells it`() = runTest {
        // Given padlock 1, its progress that cannot be written, as a full disk
        padlocks.failure = IOException("No space left on device")
        val viewModel = viewModel(level = 1)

        // When
        viewModel.opening()
        viewModel.restart()
        val restarted = viewModel.uiState.value
        viewModel.opening()

        // Then
        assertThat(uncaught).isEmpty()
        assertThat(restarted.unrecorded).isFalse
        assertThat(viewModel.uiState.value.unrecorded).isTrue
        assertThat(viewModel.uiState.value.game.isOpen).isTrue
    }

    @Test
    fun `what the opening was for the progress is kept for the card, until the padlock starts over`() = runTest {
        // Given padlock 1 opened before with a hint
        val pieces = PadlockGame.start(Padlock.ofLevel(1)).given.size
        padlocks.record(1, PadlockOpening(pieces = pieces, moves = pieces, refusals = 0, hints = 1))
        val viewModel = viewModel(level = 1)

        // When it is opened without one
        viewModel.opening()
        val opened = viewModel.uiState.value.outcome
        viewModel.restart()

        // Then
        assertThat(opened).isEqualTo(OpeningOutcome.NEW_BEST)
        assertThat(viewModel.uiState.value.outcome).isNull()
    }

    @Test
    fun `the screen follows the best opening kept of its padlock, and the frontier`() = runTest {
        // Given padlocks 1 and 2 open, padlock 1 with a hint
        val first = openingOf(level = 1, hints = 1)
        padlocks.record(1, first)
        padlocks.record(2, openingOf(level = 2))
        val viewModel = viewModel(level = 1)
        val before = viewModel.uiState.value

        // When padlock 1 is opened without a hint
        viewModel.opening()

        // Then
        assertThat(before.best).isEqualTo(first.toResult())
        assertThat(before.frontier).isEqualTo(3)
        assertThat(viewModel.uiState.value.best).isEqualTo(viewModel.uiState.value.game.result)
    }

    @Test
    fun `a padlock not open has no best opening`() = runTest {
        // When
        val viewModel = viewModel(level = 1)

        // Then
        assertThat(viewModel.uiState.value.best).isNull()
        assertThat(viewModel.uiState.value.frontier).isEqualTo(1)
    }

    @Test
    fun `a progress of a later version is read once by the screen and once per opening, the game going on`() = runTest {
        // Given a progress that cannot be read nor written, as one a later BloX wrote: empty, once, then the flow ends
        var reads = 0
        val later = object : PadlockProgressRepository {
            override val progress = flow {
                reads++
                emit(PadlockProgress())
            }

            override suspend fun record(
                padlock: Int,
                opening: PadlockOpening,
            ): OpeningOutcome = throw IOException("The padlocks file is of version 2, later than 1")

            override suspend fun clear(): Unit = throw IOException("The padlocks file is of version 2, later than 1")
        }
        val viewModel = locksmithViewModel(LocksmithRoute(level = 1), progress = later, appScope = appScope)

        // When padlock 1 is opened, replayed and opened again
        viewModel.opening()
        viewModel.restart()
        viewModel.opening()

        // Then
        assertThat(uncaught).isEmpty()
        assertThat(viewModel.uiState.value.unrecorded).isTrue
        assertThat(reads).isEqualTo(3)
    }

    @Test
    fun `the frontier is read when the opening is written, not when the screen opened`() = runTest {
        // Given a progress read once by each screen, as a file that cannot be followed, and slow to write
        val snapshots = object : PadlockProgressRepository by padlocks {
            override val progress = flow { emit(padlocks.progress.first()) }

            override suspend fun record(
                padlock: Int,
                opening: PadlockOpening,
            ): OpeningOutcome {
                delay(WRITE_MILLIS)
                return padlocks.record(padlock, opening)
            }
        }
        val later = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val first = locksmithViewModel(LocksmithRoute(level = 1), progress = snapshots, appScope = later)
        first.opening()

        // When padlock 2 opens on a screen that read the frontier while padlock 1 was still being written
        val second = locksmithViewModel(LocksmithRoute(level = 2), progress = snapshots, appScope = later)
        advanceUntilIdle()
        second.opening()
        advanceUntilIdle()

        // Then
        assertThat(second.uiState.value.unrecorded).isFalse
        assertThat(padlocks.progress.first().openings).hasSize(2)
    }

    @Test
    fun `a write that ends after the padlock started over tells nothing of the game started over`() = runTest {
        // Given padlock 1, its progress slow to write and full
        val slow = object : PadlockProgressRepository by padlocks {
            override suspend fun record(
                padlock: Int,
                opening: PadlockOpening,
            ): OpeningOutcome {
                delay(WRITE_MILLIS)
                throw IOException("No space left on device")
            }
        }
        val later = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = locksmithViewModel(LocksmithRoute(level = 1), progress = slow, appScope = later)

        // When it opens, and starts over before its opening is written
        viewModel.opening()
        viewModel.restart()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value.unrecorded).isFalse
        assertThat(viewModel.uiState.value.outcome).isNull()
    }

    @Test
    fun `the frontier is followed after a first opening, until the card offers to resume`() = runTest {
        // Given padlock 1 opened before, replayed: its next one, padlock 2, is the frontier
        padlocks.record(1, openingOf(level = 1, hints = 1))
        val viewModel = viewModel(level = 1)
        viewModel.opening()
        val replayed = openCardOf(viewModel.uiState.value)

        // When padlock 2 is opened, as on another screen
        padlocks.record(2, openingOf(level = 2))

        // Then
        assertThat(replayed?.resumeAt).isNull()
        assertThat(viewModel.uiState.value.frontier).isEqualTo(3)
        assertThat(openCardOf(viewModel.uiState.value)?.resumeAt).isEqualTo(3)
    }

    private fun viewModel(level: Int) =
        locksmithViewModel(LocksmithRoute(level = level), progress = padlocks, appScope = appScope)

    /** An opening of padlock [level], in as many moves as pieces, without refusal, with [hints]. */
    private fun openingOf(
        level: Int,
        hints: Int = 0,
    ): PadlockOpening {
        val pieces = PadlockGame.start(Padlock.ofLevel(level)).given.size
        return PadlockOpening(pieces = pieces, moves = pieces, refusals = 0, hints = hints)
    }

    /** Opens the padlock of this view model, its solution placed piece by piece. */
    private fun LocksmithViewModel.opening() {
        PadlockGame.start(Padlock.ofLevel(uiState.value.level)).padlock.solution.forEach { placing(it) }
    }

    private companion object {
        /** How long a slow write takes. */
        const val WRITE_MILLIS = 100L
    }
}
