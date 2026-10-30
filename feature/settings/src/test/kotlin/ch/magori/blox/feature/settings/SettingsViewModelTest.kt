package ch.magori.blox.feature.settings

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.haptics.FakeHaptics
import ch.magori.blox.core.haptics.Haptic
import ch.magori.blox.core.music.SoundEffects
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.IOException

class SettingsViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun `the settings page is loading until the saved settings are read, then shows them`() = runTest {
        // Given settings not read yet
        val preferences = FakeUserPreferencesRepository(initial = null)
        val viewModel = settingsViewModel(preferences)

        viewModel.uiState.test {
            // Then no switch is shown, off for a moment, before they are read
            assertThat(awaitItem()).isNull()

            // When they are read
            preferences.state.value = UserPreferences.DEFAULT.copy(vibrations = true, soundEffects = false)

            // Then
            val shown = awaitItem()
            assertThat(shown?.vibrations).isTrue()
            assertThat(shown?.soundEffects).isFalse()
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
        val viewModel = settingsViewModel(FakeUserPreferencesRepository(initial = saved))

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
        val viewModel = settingsViewModel(preferences)

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
    fun `turning the reduced effects on saves them on and shows them on, then off again`() = runTest {
        // Given the full effects, as by default
        val preferences = FakeUserPreferencesRepository()
        val viewModel = settingsViewModel(preferences)

        viewModel.uiState.test {
            // When
            viewModel.onReducedEffectsChanged(true)

            // Then
            assertThat(preferences.state.value?.reducedEffects).isTrue()
            assertThat(expectMostRecentItem()?.reducedEffects).isTrue()

            // When
            viewModel.onReducedEffectsChanged(false)

            // Then
            assertThat(preferences.state.value?.reducedEffects).isFalse()
            assertThat(expectMostRecentItem()?.reducedEffects).isFalse()
        }
    }

    @Test
    fun `turning left handed on saves it on and shows it on, then off again`() = runTest {
        // Given a right handed player, as by default
        val preferences = FakeUserPreferencesRepository()
        val viewModel = settingsViewModel(preferences)

        viewModel.uiState.test {
            assertThat(awaitItem()?.leftHanded).isFalse()

            // When
            viewModel.onLeftHandedChanged(true)

            // Then
            assertThat(preferences.state.value?.leftHanded).isTrue()
            assertThat(expectMostRecentItem()?.leftHanded).isTrue()

            // When
            viewModel.onLeftHandedChanged(false)

            // Then
            assertThat(preferences.state.value?.leftHanded).isFalse()
            assertThat(expectMostRecentItem()?.leftHanded).isFalse()
        }
    }

    @Test
    fun `turning the sound effects on saves them on and shows them on, then off again`() = runTest {
        // Given the sound effects turned off by the player: they are on by default
        val preferences = FakeUserPreferencesRepository(initial = UserPreferences.DEFAULT.copy(soundEffects = false))
        val viewModel = settingsViewModel(preferences)

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
        val viewModel = settingsViewModel(FakeUserPreferencesRepository(failWrites = true))

        viewModel.uiState.test {
            // When each switch is turned the other way
            viewModel.onVibrationsChanged(true)
            viewModel.onSoundEffectsChanged(false)

            // Then
            val shown = expectMostRecentItem()
            assertThat(shown?.vibrations).isFalse()
            assertThat(shown?.soundEffects).isTrue()
        }
    }

    @Test
    fun `turning the sound effects on plays the rotation once`() = runTest {
        // Given the sound effects turned off by the player
        val effects = FakeSoundEffectPlayer()
        val preferences = FakeUserPreferencesRepository(initial = UserPreferences.DEFAULT.copy(soundEffects = false))
        val viewModel = settingsViewModel(preferences, effects = effects)

        // When
        viewModel.onSoundEffectsChanged(true)

        // Then the player hears what they turned on, once
        assertThat(effects.played).containsExactly(SoundEffects.rotation)
    }

    @Test
    fun `a setting turned off, or that cannot be saved, is neither heard nor felt`() = runTest {
        // Given each setting on, then a disk that refuses every write, each setting off
        val effects = FakeSoundEffectPlayer()
        val haptics = FakeHaptics()
        val on = UserPreferences.DEFAULT.copy(soundEffects = true, vibrations = true, keyVibration = true)
        val turnedOff =
            settingsViewModel(FakeUserPreferencesRepository(initial = on), effects = effects, haptics = haptics)
        val failing = settingsViewModel(
            FakeUserPreferencesRepository(
                initial = UserPreferences.DEFAULT.copy(soundEffects = false),
                failWrites = true,
            ),
            effects = effects,
            haptics = haptics,
        )

        // When the first are turned off, the others on
        turnedOff.onSoundEffectsChanged(false)
        turnedOff.onVibrationsChanged(false)
        turnedOff.onKeyVibrationChanged(false)
        failing.onSoundEffectsChanged(true)
        failing.onVibrationsChanged(true)
        failing.onKeyVibrationChanged(true)

        // Then
        assertThat(effects.played).isEmpty()
        assertThat(haptics.played).isEmpty()
    }

    @Test
    fun `both volumes are shown as saved, in percent`() = runTest {
        // Given
        val saved = UserPreferences.DEFAULT.copy(effectsVolume = Volume(30), musicVolume = Volume(70))
        val viewModel = settingsViewModel(FakeUserPreferencesRepository(initial = saved))

        viewModel.uiState.test {
            // Then
            val shown = expectMostRecentItem()
            assertThat(shown?.effectsVolume).isEqualTo(30)
            assertThat(shown?.musicVolume).isEqualTo(70)
        }
    }

    @Test
    fun `a volume moved is saved as the volume of its percent, and shown so`() = runTest {
        // Given both volumes full, as by default
        val preferences = FakeUserPreferencesRepository()
        val viewModel = settingsViewModel(preferences)

        viewModel.uiState.test {
            // When
            viewModel.onEffectsVolumeChanged(40)
            viewModel.onMusicVolumeChanged(0)

            // Then
            assertThat(preferences.state.value?.effectsVolume).isEqualTo(Volume(40))
            assertThat(preferences.state.value?.musicVolume).isEqualTo(Volume.SILENT)
            val shown = expectMostRecentItem()
            assertThat(shown?.effectsVolume).isEqualTo(40)
            assertThat(shown?.musicVolume).isEqualTo(0)
        }
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `the page knows whether the device can vibrate`(canVibrate: Boolean) = runTest {
        // Given
        val viewModel = settingsViewModel(haptics = FakeHaptics(canVibrate = canVibrate))

        viewModel.uiState.test {
            // Then
            assertThat(expectMostRecentItem()?.canVibrate).isEqualTo(canVibrate)
        }
    }

    @Test
    fun `clearing the record asks first, and keeping it changes nothing`() = runTest {
        // Given a record
        val highScores = FakeHighScoreRepository(initial = 12_340)
        val viewModel = settingsViewModel(highScores = highScores)

        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.NONE)

            // When the player asks to clear it
            viewModel.onClearRecord()

            // Then the page asks first
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.ASKED)
            assertThat(highScores.record.value).isEqualTo(12_340)

            // When they keep it
            viewModel.onKeepRecord()

            // Then
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.NONE)
            assertThat(highScores.record.value).isEqualTo(12_340)
        }
    }

    @Test
    fun `a confirmed clear clears the record and says « Record effacé »`() = runTest {
        // Given a record, and the window that asks whether to clear it
        val highScores = FakeHighScoreRepository(initial = 12_340)
        val viewModel = settingsViewModel(highScores = highScores)

        viewModel.uiState.test {
            viewModel.onClearRecord()

            // When the player confirms
            viewModel.onClearRecordConfirmed()

            // Then the window closes on « Record effacé »
            assertThat(highScores.record.value).isZero()
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.CLEARED)
        }
    }

    @Test
    fun `a clear that cannot be written keeps the window open and says it failed`() = runTest {
        // Given a record that cannot be written, and the window that asks whether to clear it
        val highScores = FakeHighScoreRepository(initial = 12_340).apply { failure = IOException("The disk is full") }
        val viewModel = settingsViewModel(highScores = highScores)

        viewModel.uiState.test {
            viewModel.onClearRecord()

            // When the player confirms
            viewModel.onClearRecordConfirmed()

            // Then the window stays open and says it failed, the record kept
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.FAILED)
            assertThat(highScores.record.value).isEqualTo(12_340)

            // When the disk takes it again, and the player confirms again
            highScores.failure = null
            viewModel.onClearRecordConfirmed()

            // Then
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.CLEARED)
            assertThat(highScores.record.value).isZero()
        }
    }

    @Test
    fun `while the record is being cleared, the window stays open and answers nothing more`() = runTest {
        // Given a record whose clear takes its time, and the window that asks whether to clear it
        val gate = CompletableDeferred<Unit>()
        val highScores = FakeHighScoreRepository(initial = 12_340).apply { this.gate = gate }
        val viewModel = settingsViewModel(highScores = highScores)

        viewModel.uiState.test {
            viewModel.onClearRecord()

            // When the player confirms, then, while it is written, keeps the record and confirms again
            viewModel.onClearRecordConfirmed()
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.CLEARING)
            viewModel.onKeepRecord()
            viewModel.onClearRecordConfirmed()

            // Then the window waits, the record cleared once
            assertThat(viewModel.uiState.value?.recordClearing).isEqualTo(RecordClearing.CLEARING)
            assertThat(highScores.clears).isEqualTo(1)

            // When the clear is written
            gate.complete(Unit)

            // Then
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.CLEARED)
            assertThat(highScores.clears).isEqualTo(1)
        }
    }

    @Test
    fun `a clear is confirmed only from the window that asks`() = runTest {
        // Given a record, no window open
        val highScores = FakeHighScoreRepository(initial = 12_340)
        val viewModel = settingsViewModel(highScores = highScores)

        viewModel.uiState.test {
            // When a confirmation comes without the window (a late tap)
            viewModel.onClearRecordConfirmed()

            // Then nothing is cleared
            assertThat(expectMostRecentItem()?.recordClearing).isEqualTo(RecordClearing.NONE)
            assertThat(highScores.clears).isZero()
            assertThat(highScores.record.value).isEqualTo(12_340)
        }
    }

    @Test
    fun `a volume that cannot be saved leaves the saved volume shown, without crashing`() = runTest {
        // Given a disk that refuses every write, both volumes full, as by default
        val viewModel = settingsViewModel(FakeUserPreferencesRepository(failWrites = true))

        viewModel.uiState.test {
            // When each volume is moved
            viewModel.onEffectsVolumeChanged(30)
            viewModel.onMusicVolumeChanged(70)

            // Then
            val shown = expectMostRecentItem()
            assertThat(shown?.effectsVolume).isEqualTo(100)
            assertThat(shown?.musicVolume).isEqualTo(100)
        }
    }

    @ParameterizedTest
    @CsvSource("VIBRATIONS, LINES_CLEARED", "KEY_VIBRATION, KEY_PRESS")
    fun `turning a vibration on vibrates once`(
        setting: VibrationSetting,
        felt: Haptic,
    ) = runTest {
        // Given both vibrations off, as by default
        val haptics = FakeHaptics()
        val viewModel = settingsViewModel(haptics = haptics)

        // When
        setting.turn(viewModel, true)

        // Then the player feels what they turned on, once: lines cleared, or a key pressed
        assertThat(haptics.played).containsExactly(felt)
    }

    @Test
    fun `turning the key vibration on saves it on and shows it on, then off again`() = runTest {
        // Given the keys that do not vibrate, as by default
        val preferences = FakeUserPreferencesRepository()
        val viewModel = settingsViewModel(preferences)

        viewModel.uiState.test {
            // When
            viewModel.onKeyVibrationChanged(true)

            // Then
            assertThat(preferences.state.value?.keyVibration).isTrue()
            assertThat(expectMostRecentItem()?.keyVibration).isTrue()

            // When
            viewModel.onKeyVibrationChanged(false)

            // Then
            assertThat(preferences.state.value?.keyVibration).isFalse()
            assertThat(expectMostRecentItem()?.keyVibration).isFalse()
        }
    }

    @Test
    fun `the factory builds the settings page on the settings of the application`() = runTest {
        // Given the settings of the application, the vibrations on
        val saved = UserPreferences.DEFAULT.copy(vibrations = true)
        val factory = SettingsViewModel.Factory(
            FakeUserPreferencesRepository(initial = saved),
            FakeHighScoreRepository(),
            FakeSoundEffectPlayer(),
            FakeHaptics(),
        )

        // When
        val viewModel = ViewModelProvider.create(ViewModelStore(), factory)[SettingsViewModel::class]

        // Then
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.vibrations).isTrue()
        }
    }

    /** The settings page on [preferences], [highScores], [effects] and [haptics]. */
    private fun settingsViewModel(
        preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
        highScores: FakeHighScoreRepository = FakeHighScoreRepository(),
        effects: FakeSoundEffectPlayer = FakeSoundEffectPlayer(),
        haptics: FakeHaptics = FakeHaptics(),
    ) = SettingsViewModel(preferences, highScores, effects, haptics)
}

/** The two vibrations of the settings, each turned on or off by its own switch. */
enum class VibrationSetting(val turn: (SettingsViewModel, Boolean) -> Unit) {
    VIBRATIONS(SettingsViewModel::onVibrationsChanged),
    KEY_VIBRATION(SettingsViewModel::onKeyVibrationChanged),
}
