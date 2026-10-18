package ch.magori.blox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.game.rules.LevelProgression
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.abs
import kotlin.reflect.KClass

/**
 * The home screen (V1): the saved settings, the record, and the choices of the player, saved as soon
 * as they are made. The start levels offered are those of the mode, from its [LevelProgression]; a
 * saved level out of them (saved by another mode) is shown as the closest of them.
 */
class HomeViewModel(
    private val preferences: UserPreferencesRepository,
    highScores: HighScoreRepository,
    levelProgression: LevelProgression,
) : ViewModel() {
    private val startLevels: List<Int> = levelProgression.startLevels.map { it.value }

    init {
        require(startLevels.isNotEmpty()) { "A mode offers at least one start level" }
    }

    /** What the screen shows; null while the saved settings are not read yet, so that nothing flashes. */
    internal val uiState: StateFlow<HomeUiState?> =
        combine(preferences.preferences, highScores.highScore, ::toUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

    internal fun onThemeChosen(theme: ThemeChoice) = save { setTheme(theme) }

    internal fun onMusicChosen(music: MusicChoice) = save { setMusic(music) }

    /** Saves [startLevel] if the mode offers it; any other level is ignored. */
    internal fun onStartLevelChosen(startLevel: Int) {
        if (startLevel in startLevels) save { setStartLevel(startLevel) }
    }

    /**
     * Saves a choice. A write that fails (a full disk) is dropped: the screen keeps showing the saved
     * settings, and the player may choose again.
     */
    private fun save(write: suspend UserPreferencesRepository.() -> Unit) {
        viewModelScope.launch {
            try {
                preferences.write()
            } catch (_: IOException) {
                // Nothing saved: the settings shown are still the saved ones.
            }
        }
    }

    private fun toUiState(
        saved: UserPreferences,
        record: Int,
    ): HomeUiState = HomeUiState(
        theme = saved.theme,
        music = saved.music,
        startLevel = startLevels.minBy { abs(it - saved.startLevel) },
        startLevels = startLevels,
        record = record,
    )

    /**
     * Builds the [HomeViewModel] with the repositories and the level progression of the application,
     * which builds this factory (ADR 0013).
     */
    class Factory(
        private val preferences: UserPreferencesRepository,
        private val highScores: HighScoreRepository,
        private val levelProgression: LevelProgression,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(preferences, highScores, levelProgression) as T
        }
    }

    private companion object {
        /** Keeps the settings flowing across a configuration change, as the Android guidance advises. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
