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
import ch.magori.blox.core.data.MusicFileRepository
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.game.rules.LevelProgression
import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
 * plays on too; [scoreOf] gives the score of a music choice, null for none (`app` defines it). « Mon
 * fichier » plays the music file the player picked in the picker of the system, kept in [musicFiles]; it
 * may be listened to once there is one. A music of the player chosen for the games in [library] (« Mes musiques ») is
 * shown as the music selected, the games playing it; choosing another music leaves it.
 */
class HomeViewModel(
    private val preferences: UserPreferencesRepository,
    highScores: HighScoreRepository,
    levelProgression: LevelProgression,
    private val music: MusicPlayer,
    private val scoreOf: (MusicChoice) -> Score?,
    private val musicFiles: MusicFileRepository,
    private val library: MusicLibrary,
) : ViewModel() {
    private val startLevels: List<Int> = levelProgression.startLevels.map { it.value }

    /**
     * The music file the player picked and the music of the player chosen for the games, the only readings of
     * [musicFiles] and of [library], followed for as long as the screen lives; null until both are read.
     */
    private val sources: StateFlow<MusicSources?> =
        combine(musicFiles.musicFile, library.gameMusic, ::MusicSources)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** The music file the player picked, once read; null while there is none, or before it is read. */
    private val musicFile: MusicFile? get() = sources.value?.file

    private val pickerRequested = MutableStateFlow(false)

    /**
     * Whether the screen is to open the picker of the system for « Mon fichier »: true until it says it opened it
     * ([onMusicFilePickerOpened]).
     */
    internal val musicFilePickerRequested: StateFlow<Boolean> = pickerRequested.asStateFlow()

    /** The music being listened to, if any. */
    private val previewing = MutableStateFlow<Previewed?>(null)

    /** The playback of the music being listened to: stopping it never stops a music started since. */
    private var preview: Playback? = null

    /** Follows the [preview], to offer to listen again once it stops by itself. */
    private var previewWatch: Job? = null

    init {
        require(startLevels.isNotEmpty()) { "A mode offers at least one start level" }
        // A music of the player listened to stops once it is no longer the one of the games: deleted, or replaced,
        // from another screen. The home screen no longer shows it, nor its button to stop it.
        viewModelScope.launch {
            sources.filterNotNull().collect { current ->
                val listened = previewing.value
                if (listened is Previewed.Personal && listened.id != current.gameMusic?.id) stopPreview()
            }
        }
    }

    /**
     * What the screen shows; null while the saved settings, the music file and the music for the games are not read
     * yet, so that nothing flashes, nor a listen button or a choice shows up late.
     */
    internal val uiState: StateFlow<HomeUiState?> =
        combine(preferences.preferences, highScores.highScore, previewing, sources.filterNotNull(), ::toUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

    internal fun onThemeChosen(theme: ThemeChoice) = save { setTheme(theme) }

    /**
     * Saves [music], which the games play from now on instead of the music of the player chosen for them, if any; the
     * music being listened to, if another one, stops. « Mon fichier » without a music file opens the picker instead: it
     * is chosen once a file is picked.
     */
    internal fun onMusicChosen(music: MusicChoice) {
        if (music == MusicChoice.MY_FILE && musicFile == null) {
            pickerRequested.value = true
            return
        }
        if (Previewed.Choice(music) != previewing.value) stopPreview()
        save { choose(music) }
    }

    /** Saves [startLevel] if the mode offers it; any other level is ignored. */
    internal fun onStartLevelChosen(startLevel: Int) {
        if (startLevel in startLevels) save { setStartLevel(startLevel) }
    }

    /**
     * The player picked [uri] in the picker of the system, opened by « Mon fichier »: the file is kept, then
     * « Mon fichier » chosen, as [onMusicChosen] does; the music being listened to stops, the file being another music.
     * A file that cannot be kept leaves the music chosen before. A picker cancelled tells nothing: the choice stays.
     */
    internal fun onMusicFileChosen(uri: String) {
        stopPreview()
        save {
            musicFiles.setMusicFile(MusicFile(uri))
            choose(MusicChoice.MY_FILE)
        }
    }

    /** The player wants another music file (« Changer de fichier »): the picker opens. */
    internal fun onChangeMusicFile() {
        pickerRequested.value = true
    }

    /** The screen opened the picker it was asked for: it is asked for no more. */
    internal fun onMusicFilePickerOpened() {
        pickerRequested.value = false
    }

    /** The player listens to [listened], before playing. */
    internal fun onListen(listened: MusicChoice) {
        val listenedMusic = listenedMusicOf(listened, musicFile) ?: return
        listen(Previewed.Choice(listened), listenedMusic)
    }

    /** The player listens to the music of the player chosen for the games, its score, before playing; if there is one. */
    internal fun onListenGameMusic() {
        val gameMusic = sources.value?.gameMusic ?: return
        listen(Previewed.Personal(gameMusic.id), gameMusic.score)
    }

    /** Plays [listenedMusic], known on the screen as [listened]; or stops it, if it is being listened to already. */
    private fun listen(
        listened: Previewed,
        listenedMusic: Music,
    ) {
        val again = previewing.value == listened
        stopPreview()
        if (!again) {
            val playback = music.play(listenedMusic)
            preview = playback
            previewing.value = listened
            previewWatch = viewModelScope.launch { watch(playback) }
        }
    }

    /** The player starts a game: the music being listened to stops, the game plays its own. */
    internal fun onPlay() = stopPreview()

    /** The player opens the music editor: the music being listened to stops, the editor plays its own. */
    internal fun onCompose() = stopPreview()

    /** The player opens « Mes musiques »: the music being listened to stops, the list plays its own. */
    internal fun onMyMusics() = stopPreview()

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

    /**
     * Chooses [music] on the home screen, and leaves the music of the player chosen for the games: in the mockup (V1), it
     * is one of the music choices, of which one only is selected. The home screen music is saved first: if the library
     * then fails to be written, the music of the player stays selected, as before.
     */
    private suspend fun choose(music: MusicChoice) {
        preferences.setMusic(music)
        library.setGameMusic(null)
    }

    private fun toUiState(
        saved: UserPreferences,
        record: Int,
        previewed: Previewed?,
        sources: MusicSources,
    ): HomeUiState = HomeUiState(
        theme = saved.theme,
        music = saved.music,
        startLevel = startLevels.minBy { abs(it - saved.startLevel) },
        startLevels = startLevels,
        record = record,
        listenable = MusicChoice.entries.filter { listenedMusicOf(it, sources.file) != null },
        previewing = (previewed as? Previewed.Choice)?.choice,
        hasMusicFile = sources.file != null,
        gameMusic = sources.gameMusic?.let { GameMusicUi(it.name, previewed = previewed == Previewed.Personal(it.id)) },
    )

    /**
     * What [choice] plays when it is listened to: its BloX score, or, for « Mon fichier », the music [file] picked;
     * null when there is nothing to listen to.
     */
    private fun listenedMusicOf(
        choice: MusicChoice,
        file: MusicFile?,
    ): Music? = if (choice == MusicChoice.MY_FILE) file else scoreOf(choice)

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
        private val musicFiles: MusicFileRepository,
        private val library: MusicLibrary,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(preferences, highScores, levelProgression, music, scoreOf, musicFiles, library) as T
        }
    }

    /**
     * The music [file] the player picked, null when there is none, and the music of the player played during the games,
     * [gameMusic], null when the games play the music chosen on the home screen.
     */
    private data class MusicSources(val file: MusicFile?, val gameMusic: SavedMusic?)

    /** A music being listened to: a [Choice] of the home screen, or the music of the player of [Personal] identity. */
    private sealed interface Previewed {
        data class Choice(val choice: MusicChoice) : Previewed

        data class Personal(val id: ScoreId.Personal) : Previewed
    }

    private companion object {
        /** Keeps the settings flowing across a configuration change, as the Android guidance advises. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
