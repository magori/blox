package ch.magori.blox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.haptics.Haptic
import ch.magori.blox.core.haptics.Haptics
import ch.magori.blox.core.music.SoundEffects
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.reflect.KClass

/**
 * The settings page (V21, V31): the settings of the games, as saved in [preferences], and the changes of the player,
 * saved as soon as they are made. A game reads the settings once, when it is created: a change made here holds for the
 * next game, never for one under way. A setting turned on is felt once, as soon as it is saved: the rotation on
 * [soundEffects], a vibration on [haptics]. The record kept in [highScores] is cleared only once the player confirms.
 */
class SettingsViewModel(
    private val preferences: UserPreferencesRepository,
    private val highScores: HighScoreRepository,
    private val soundEffects: SoundEffectPlayer,
    private val haptics: Haptics,
) : ViewModel() {
    /** Where the clearing of the record stands. */
    private val recordClearing = MutableStateFlow(RecordClearing.NONE)

    /** What the page shows; null while the saved settings are not read yet, so that no switch flashes off. */
    internal val uiState: StateFlow<SettingsUiState?> =
        combine(preferences.preferences, recordClearing) { saved, clearing ->
            SettingsUiState(
                vibrations = saved.vibrations,
                soundEffects = saved.soundEffects,
                keyVibration = saved.keyVibration,
                effectsVolume = saved.effectsVolume.percent,
                musicVolume = saved.musicVolume.percent,
                canVibrate = haptics.canVibrate,
                recordClearing = clearing,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null,
        )

    /** Saves whether the games play their sound effects; turned on, the rotation plays once. */
    internal fun onSoundEffectsChanged(soundEffects: Boolean) =
        save(onSaved = { if (soundEffects) this.soundEffects.play(SoundEffects.rotation) }) {
            setSoundEffects(soundEffects)
        }

    /** Saves the volume of the sound effects, [percent] of the full volume (a step of the slider). */
    internal fun onEffectsVolumeChanged(percent: Int) = save { setEffectsVolume(Volume(percent)) }

    /** Saves the volume of the music, [percent] of the full volume (a step of the slider). */
    internal fun onMusicVolumeChanged(percent: Int) = save { setMusicVolume(Volume(percent)) }

    /** Saves whether the games vibrate; turned on, the vibration of lines cleared is felt once. */
    internal fun onVibrationsChanged(vibrations: Boolean) =
        save(onSaved = { if (vibrations) haptics.play(Haptic.LINES_CLEARED) }) { setVibrations(vibrations) }

    /** Saves whether each key of the games vibrates when pressed; turned on, a key press is felt once. */
    internal fun onKeyVibrationChanged(keyVibration: Boolean) =
        save(onSaved = { if (keyVibration) haptics.play(Haptic.KEY_PRESS) }) { setKeyVibration(keyVibration) }

    /** Asks whether to clear the record: the window opens. */
    internal fun onClearRecord() {
        if (recordClearing.value != RecordClearing.CLEARING) recordClearing.value = RecordClearing.ASKED
    }

    /** Keeps the record: the window closes, nothing changed; not while the clear is being written. */
    internal fun onKeepRecord() {
        if (recordClearing.value != RecordClearing.CLEARING) recordClearing.value = RecordClearing.NONE
    }

    /**
     * Clears the record, as the player confirmed in the window (asked, or after a failure; any other confirmation, a
     * late tap, is ignored). While it is written, the window stays open and answers nothing; then it closes on « Record
     * effacé ». A record that cannot be written (a full disk) is kept, and the window stays open to say so, for the
     * player to try again or keep it.
     */
    internal fun onClearRecordConfirmed() {
        if (recordClearing.value != RecordClearing.ASKED && recordClearing.value != RecordClearing.FAILED) return
        recordClearing.value = RecordClearing.CLEARING
        viewModelScope.launch {
            recordClearing.value = try {
                highScores.clear()
                RecordClearing.CLEARED
            } catch (_: IOException) {
                RecordClearing.FAILED
            }
        }
    }

    /**
     * Saves a setting, then calls [onSaved]. A write that fails (a full disk) is dropped, [onSaved] not called: the page
     * keeps showing the saved settings, and the player may try again.
     */
    private fun save(
        onSaved: () -> Unit = {},
        write: suspend UserPreferencesRepository.() -> Unit,
    ) = viewModelScope.saveSettings(preferences, write, onSaved)

    /**
     * Builds the [SettingsViewModel] on the settings and the record of the application, its sound effects and its
     * vibrations; the application builds this factory (ADR 0013).
     */
    class Factory(
        private val preferences: UserPreferencesRepository,
        private val highScores: HighScoreRepository,
        private val soundEffects: SoundEffectPlayer,
        private val haptics: Haptics,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferences, highScores, soundEffects, haptics) as T
        }
    }

    private companion object {
        /** Keeps the settings flowing across a configuration change, as the Android guidance advises. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
