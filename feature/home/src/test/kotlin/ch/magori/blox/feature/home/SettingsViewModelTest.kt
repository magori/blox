package ch.magori.blox.feature.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.UserPreferences
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SettingsViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun `the settings page is loading until the saved settings are read, then shows them`() = runTest {
        // Given settings not read yet
        val preferences = FakeUserPreferencesRepository(initial = null)
        val viewModel = SettingsViewModel(preferences)

        viewModel.uiState.test {
            // Then no switch is shown, off for a moment, before they are read
            assertThat(awaitItem()).isNull()

            // When they are read
            preferences.state.value = UserPreferences.DEFAULT.copy(vibrations = true, soundEffects = false)

            // Then
            assertThat(awaitItem()).isEqualTo(SettingsUiState(vibrations = true, soundEffects = false))
        }
    }

    @ParameterizedTest
    @CsvSource("false, false", "true, false", "false, true", "true, true")
    fun `the vibrations and the sound effects are shown as saved`(
        vibrations: Boolean,
        soundEffects: Boolean,
    ) = runTest {
        // Given
        val saved = UserPreferences.DEFAULT.copy(vibrations = vibrations, soundEffects = soundEffects)
        val viewModel = SettingsViewModel(FakeUserPreferencesRepository(initial = saved))

        viewModel.uiState.test {
            // Then
            val shown = expectMostRecentItem()
            assertThat(shown?.vibrations).isEqualTo(vibrations)
            assertThat(shown?.soundEffects).isEqualTo(soundEffects)
        }
    }

    @Test
    fun `turning the vibrations on saves them on and shows them on, then off again`() = runTest {
        // Given the vibrations off, as by default
        val preferences = FakeUserPreferencesRepository()
        val viewModel = SettingsViewModel(preferences)

        viewModel.uiState.test {
            // When
            viewModel.onVibrationsChanged(true)

            // Then
            assertThat(preferences.state.value?.vibrations).isTrue()
            assertThat(expectMostRecentItem()?.vibrations).isTrue()

            // When
            viewModel.onVibrationsChanged(false)

            // Then
            assertThat(preferences.state.value?.vibrations).isFalse()
            assertThat(expectMostRecentItem()?.vibrations).isFalse()
        }
    }

    @Test
    fun `turning the sound effects on saves them on and shows them on, then off again`() = runTest {
        // Given the sound effects turned off by the player: they are on by default
        val preferences = FakeUserPreferencesRepository(initial = UserPreferences.DEFAULT.copy(soundEffects = false))
        val viewModel = SettingsViewModel(preferences)

        viewModel.uiState.test {
            // When
            viewModel.onSoundEffectsChanged(true)

            // Then
            assertThat(preferences.state.value?.soundEffects).isTrue()
            assertThat(expectMostRecentItem()?.soundEffects).isTrue()

            // When
            viewModel.onSoundEffectsChanged(false)

            // Then
            assertThat(preferences.state.value?.soundEffects).isFalse()
            assertThat(expectMostRecentItem()?.soundEffects).isFalse()
        }
    }

    @Test
    fun `a setting that cannot be saved leaves the saved settings shown, without crashing`() = runTest {
        // Given a disk that refuses every write, the settings at their defaults: vibrations off, sound effects on
        val viewModel = SettingsViewModel(FakeUserPreferencesRepository(failWrites = true))

        viewModel.uiState.test {
            // When each switch is turned the other way
            viewModel.onVibrationsChanged(true)
            viewModel.onSoundEffectsChanged(false)

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(SettingsUiState(vibrations = false, soundEffects = true))
        }
    }

    @Test
    fun `the factory builds the settings page on the settings of the application`() = runTest {
        // Given the settings of the application, the vibrations on
        val saved = UserPreferences.DEFAULT.copy(vibrations = true)
        val factory = SettingsViewModel.Factory(FakeUserPreferencesRepository(initial = saved))

        // When
        val viewModel = ViewModelProvider.create(ViewModelStore(), factory)[SettingsViewModel::class]

        // Then
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()).isEqualTo(SettingsUiState(vibrations = true, soundEffects = true))
        }
    }
}
