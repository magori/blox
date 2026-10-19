package ch.magori.blox.feature.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.game.rules.LevelProgression
import ch.magori.blox.core.music.Score
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.abs
import kotlin.reflect.KClass

/**
 * The home screen (V1): the saved settings, the record, and the choices of the player, saved as soon
 * as they are made. The start levels offered are those of the mode, from its [LevelProgression]; a
 * saved level out of them (saved by another mode) is shown as the closest of them. The player may
 * listen to a music with a BloX score before playing (« Écouter »), on the [music] player the game
 * plays on too; [scoreOf] gives the score of a music choice, null for none (`app` defines it).
 */
class HomeViewModel(
    private val preferences: UserPreferencesRepository,
    highScores: HighScoreRepository,
    levelProgression: LevelProgression,
    private val music: MusicPlayer,
    private val scoreOf: (MusicChoice) -> Score?,
) : ViewModel() {
    private val startLevels: List<Int> = levelProgression.startLevels.map { it.value }

    /** The musics with a BloX score, which the player may listen to before playing. */
    private val listenable: List<MusicChoice> = MusicChoice.entries.filter { scoreOf(it) != null }

    /** The music being listened to, if any. */
    private val previewing = MutableStateFlow<MusicChoice?>(null)

    /** The playback of the music being listened to: stopping it never stops a music started since. */
    private var preview: Playback? = null

    /** Follows the [preview], to offer to listen again once it stops by itself. */
    private var previewWatch: Job? = null

    init {
        require(startLevels.isNotEmpty()) { "A mode offers at least one start level" }
    }

    /** What the screen shows; null while the saved settings are not read yet, so that nothing flashes. */
    internal val uiState: StateFlow<HomeUiState?> =
        combine(preferences.preferences, highScores.highScore, previewing, ::toUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

    internal fun onThemeChosen(theme: ThemeChoice) = save { setTheme(theme) }

    /** Saves [music]; the music being listened to, if another one, stops. */
    internal fun onMusicChosen(music: MusicChoice) {
        if (music != previewing.value) stopPreview()
        save { setMusic(music) }
    }

    /** Saves [startLevel] if the mode offers it; any other level is ignored. */
    internal fun onStartLevelChosen(startLevel: Int) {
        if (startLevel in startLevels) save { setStartLevel(startLevel) }
    }

    /** The player listens to [listened], before playing. */
    internal fun onListen(listened: MusicChoice) {
        val score = scoreOf(listened) ?: return
        val again = previewing.value == listened
        stopPreview()
        if (!again) {
            val playback = music.play(score)
            preview = playback
            previewing.value = listened
            previewWatch = viewModelScope.launch { watch(playback) }
        }
    }

    /** The player starts a game: the music being listened to stops, the game plays its own. */
    internal fun onPlay() = stopPreview()

    /**
     * The home screen went through [event]: once it is no longer visible (`ON_STOP`: the application in
     * the background, or a game in front), the music being listened to stops. A pause alone, behind a
     * dialog or in a split screen, lets it play on.
     */
    internal fun onLifecycleEvent(event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_STOP) stopPreview()
    }

    /** The home screen leaves the stack: the music being listened to stops with it. */
    override fun onCleared() = stopPreview()

    /**
     * Waits for [playback] to stop by itself (the audio output fails, the audio focus is lost for good, the
     * headphones are unplugged; a score has no natural end, it loops), then offers to listen again: the
     * screen shows « Écouter » once more.
     */
    private suspend fun watch(playback: Playback) {
        playback.isPlaying.first { !it }
        stopPreview()
    }

    /** Stops the music being listened to, if any, and only it: a music started since keeps playing. */
    private fun stopPreview() {
        previewWatch?.cancel()
        previewWatch = null
        preview?.stop()
        preview = null
        previewing.value = null
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
        previewed: MusicChoice?,
    ): HomeUiState = HomeUiState(
        theme = saved.theme,
        music = saved.music,
        startLevel = startLevels.minBy { abs(it - saved.startLevel) },
        startLevels = startLevels,
        record = record,
        listenable = listenable,
        previewing = previewed,
    )

    /**
     * Builds the [HomeViewModel] with the repositories, the level progression and the music player of the
     * application, which builds this factory (ADR 0013): the same player as the game.
     */
    class Factory(
        private val preferences: UserPreferencesRepository,
        private val highScores: HighScoreRepository,
        private val levelProgression: LevelProgression,
        private val music: MusicPlayer,
        private val scoreOf: (MusicChoice) -> Score?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(preferences, highScores, levelProgression, music, scoreOf) as T
        }
    }

    private companion object {
        /** Keeps the settings flowing across a configuration change, as the Android guidance advises. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
