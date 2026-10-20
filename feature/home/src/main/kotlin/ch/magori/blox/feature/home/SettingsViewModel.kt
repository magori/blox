package ch.magori.blox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.reflect.KClass

/**
 * The settings page (V21): the settings of the games, as saved in [preferences], and the changes of the player, saved
 * as soon as they are made. A game reads the settings once, when it is created: a change made here holds for the next
 * game, never for one under way.
 */
class SettingsViewModel(private val preferences: UserPreferencesRepository) : ViewModel() {
    /** What the page shows; null while the saved settings are not read yet, so that no switch flashes off. */
    internal val uiState: StateFlow<SettingsUiState?> =
        preferences.preferences
            .map { SettingsUiState(vibrations = it.vibrations, soundEffects = it.soundEffects) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

    /** Saves whether the games vibrate. */
    internal fun onVibrationsChanged(vibrations: Boolean) = save { setVibrations(vibrations) }

    /** Saves whether the games play their sound effects. */
    internal fun onSoundEffectsChanged(soundEffects: Boolean) = save { setSoundEffects(soundEffects) }

    /**
     * Saves a setting. A write that fails (a full disk) is dropped: the page keeps showing the saved settings, and the
     * player may try again.
     */
    private fun save(write: suspend UserPreferencesRepository.() -> Unit) =
        viewModelScope.saveSettings(preferences, write)

    /** Builds the [SettingsViewModel] on the settings of the application, which builds this factory (ADR 0013). */
    class Factory(private val preferences: UserPreferencesRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferences) as T
        }
    }

    private companion object {
        /** Keeps the settings flowing across a configuration change, as the Android guidance advises. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
