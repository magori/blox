package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Characterization tests of [saveSettings], moved from feature:home to core:data in P12.2 (already in use). */
class SaveSettingsTest {
    @Test
    fun `a choice is written in the scope, then onSaved is called`() = runTest {
        // Given
        val preferences = FakeUserPreferencesRepository()
        var saved = 0

        // When
        preferences.saveSettings(this, write = { setVibrations(true) }, onSaved = { saved++ })
        advanceUntilIdle()

        // Then
        assertThat(preferences.state.value?.vibrations).isTrue()
        assertThat(saved).isEqualTo(1)
    }

    @Test
    fun `a write that fails as a full disk is dropped, onSaved not called, the scope still active`() = runTest {
        // Given
        val preferences = FakeUserPreferencesRepository(failWrites = true)
        val scope = CoroutineScope(coroutineContext + Job(coroutineContext[Job]))
        var saved = 0

        // When
        preferences.saveSettings(scope, write = { setVibrations(true) }, onSaved = { saved++ })
        advanceUntilIdle()

        // Then
        assertThat(preferences.state.value?.vibrations).isFalse()
        assertThat(saved).isZero()
        assertThat(scope.isActive).isTrue()
        scope.cancel()
    }

    @Test
    fun `a write cancelled with its scope does not call onSaved`() = runTest {
        // Given a write that never ends
        val preferences = FakeUserPreferencesRepository()
        val scope = CoroutineScope(coroutineContext + Job(coroutineContext[Job]))
        var saved = 0
        preferences.saveSettings(scope, write = { awaitCancellation() }, onSaved = { saved++ })
        advanceUntilIdle()

        // When
        scope.cancel()
        advanceUntilIdle()

        // Then
        assertThat(saved).isZero()
    }
}
