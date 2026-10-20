package ch.magori.blox.di

import android.content.Context
import android.util.Log
import ch.magori.blox.core.audio.AudioFilePlayer
import ch.magori.blox.core.audio.AudioTrackSink
import ch.magori.blox.core.audio.ChiptuneSequencer
import ch.magori.blox.core.audio.FocusedMusicPlayer
import ch.magori.blox.core.audio.MediaPlayerTracks
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.MusicSwitch
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.audio.SoundEffectTracks
import ch.magori.blox.core.audio.StaticAudioTrack
import ch.magori.blox.core.audio.Synthesizer
import ch.magori.blox.core.audio.SystemAudioFocus
import ch.magori.blox.core.audio.effectSampleRate
import ch.magori.blox.core.data.DataStoreHighScoreRepository
import ch.magori.blox.core.data.DataStoreMusicFileRepository
import ch.magori.blox.core.data.DataStoreMusicLibrary
import ch.magori.blox.core.data.DataStoreUserPreferencesRepository
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MUSICS_FILE
import ch.magori.blox.core.data.MY_MUSIC_FILE
import ch.magori.blox.core.data.MusicFileRepository
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.RECORDS_FILE
import ch.magori.blox.core.data.SETTINGS_FILE
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.data.preferencesDataStore
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.haptics.Haptics
import ch.magori.blox.core.haptics.SystemHaptics
import ch.magori.blox.feature.composer.ComposerViewModel
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.musics.DeletedMusicRelay
import ch.magori.blox.feature.musics.MyMusicsViewModel
import ch.magori.blox.music.FALLBACK_SCORE
import ch.magori.blox.music.gameMusicScore
import ch.magori.blox.music.musicOf
import ch.magori.blox.music.scoreOf
import ch.magori.blox.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * Builds what the features need, by hand (ADR 0007). The features define their view model
 * factories; this container builds them with the implementations of the application (ADR 0013).
 * It lives as long as the application, which creates it once: a DataStore file must have a single
 * instance per process.
 *
 * @param context the application: its private files directory, its audio focus, its vibrator.
 */
class AppContainer(context: Context) {
    private val filesDir: File = context.filesDir

    /** Where the DataStores live and work, for the whole life of the process. */
    private val dataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The record, in `files/datastore/records.preferences_pb`: the game and the home screen share it. */
    private val highScores: HighScoreRepository = DataStoreHighScoreRepository(
        preferencesDataStore(dataScope) { dataStoreFile(filesDir, RECORDS_FILE) },
    )

    /** The settings, in `files/datastore/settings.preferences_pb`, apart from the record. */
    private val userPreferences: UserPreferencesRepository = DataStoreUserPreferencesRepository(
        preferencesDataStore(dataScope) { dataStoreFile(filesDir, SETTINGS_FILE) },
    )

    /**
     * The music file of « Mon fichier », in `files/datastore/music_file.preferences_pb`, apart from the settings: the
     * backup rules leave it out, so that the uri of a document of the player never leaves the device.
     */
    private val musicFiles: MusicFileRepository = DataStoreMusicFileRepository(
        preferencesDataStore(dataScope) { dataStoreFile(filesDir, MY_MUSIC_FILE) },
    )

    /**
     * The musics of the player, « Mes musiques », in `files/datastore/musics.bin`, and the one played during the
     * games.
     * Built here, once for the process; the games read the music chosen for them from it, the home screen shows it, the
     * list « Mes musiques » shows and changes them all; the editor will receive it (P4.19d).
     */
    val musicLibrary: MusicLibrary = DataStoreMusicLibrary(dataScope) { dataStoreFile(filesDir, MUSICS_FILE) }

    /** The theme of the whole application, the one saved in the settings. */
    val appTheme: AppTheme = AppTheme(userPreferences, dataScope)

    /**
     * The music of the games and of the preview on the home screen: a single sequencer for the whole life
     * of the process, rendering on [Dispatchers.Default] and writing to an [AudioTrackSink] on
     * [Dispatchers.IO], and a single player of the music file of the player, on `MediaPlayer`, one of them
     * playing at a time. A music file that can no longer be read plays Korobeiniki instead. A failure stops
     * the music, never the game; it is logged here, the only place that knows Android. It plays only with
     * the audio focus of the system, which it gives back on stop: it never mixes with another app, and the
     * headphones unplugged stop it.
     */
    private val music: MusicPlayer = FocusedMusicPlayer(
        player = MusicSwitch(
            scores = ChiptuneSequencer(
                synthesizer = Synthesizer(MUSIC_SAMPLE_RATE),
                openSink = ::AudioTrackSink,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                onFailure = { failure -> Log.w(TAG, "The music stopped", failure) },
            ),
            files = AudioFilePlayer(
                MediaPlayerTracks(context, onFailure = { failure -> Log.w(TAG, "The music file stopped", failure) }),
            ),
            fallback = FALLBACK_SCORE,
        ),
        focus = SystemAudioFocus(context),
    )

    /**
     * The vibrations of the games, on the lines cleared and at the game over, when the settings allow them. A
     * device without a vibrator plays nothing.
     */
    private val haptics: Haptics = SystemHaptics(context)

    /** The sample rate of the sound effects: the one of the output of the device, for its fast path. */
    private val effectSampleRate: Int = effectSampleRate(context)

    /**
     * The sound effects of the games, when the settings allow them: each one rendered once by its own synthesizer, at
     * the rate of the output, and kept in a static `AudioTrack`, opened when a game warms them up, then replayed over
     * the music without the audio focus. Their work runs one piece at a time on [Dispatchers.Default], never on the
     * main thread. A failure plays nothing, never stops the game, and is logged here. The tracks, four small buffers,
     * live as long as the process, as this container: they are never closed.
     */
    private val soundEffects: SoundEffectPlayer = SoundEffectTracks(
        render = Synthesizer(effectSampleRate)::render,
        openTrack = { samples -> StaticAudioTrack(samples, effectSampleRate) },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1)),
        onFailure = { failure -> Log.w(TAG, "A sound effect could not play", failure) },
    )

    /**
     * Classic mode: one engine per game, from the seed of the game route (ADR 0014); the music of the player chosen for
     * the games, else the one chosen on the home screen; the vibrations; the sound effects.
     */
    val gameViewModelFactory: GameViewModel.Factory = GameViewModel.Factory(
        ClassicGameEngineFactory,
        highScores,
        userPreferences,
        music,
        haptics,
        soundEffects,
        musicFiles::musicOf,
        musicLibrary::gameMusicScore,
    )

    /**
     * The settings and the record kept on the device; the start levels of the Classic mode; the music
     * player of the games and the scores of the music choices, for the preview; the music file of « Mon fichier »;
     * the musics of the player, for the one chosen for the games.
     */
    val homeViewModelFactory: HomeViewModel.Factory = HomeViewModel.Factory(
        userPreferences,
        highScores,
        ClassicRules.levelProgression,
        music,
        ::scoreOf,
        musicFiles,
        musicLibrary,
    )

    /** The music editor: a music of the player, or a new one, listened to by the music player of the games. */
    val composerViewModelFactory: ComposerViewModel.Factory = ComposerViewModel.Factory(music, musicLibrary)

    /**
     * The music deleted in the editor, handed over to « Mes musiques », which offers to undo it: once for the process,
     * like the list and the editor it links.
     */
    val deletedMusics: DeletedMusicRelay = DeletedMusicRelay()

    /**
     * « Mes musiques »: the musics of the player, and the music player of the games and of the home screen, to listen to
     * them from the list; the musics deleted in the editor, to undo their deletion.
     */
    val myMusicsViewModelFactory: MyMusicsViewModel.Factory =
        MyMusicsViewModel.Factory(musicLibrary, music, deletedMusics)
}

private const val TAG = "BloX"

/** Samples per second of the music: 44 100 Hz, the rate of a CD, which every Android device plays. */
private const val MUSIC_SAMPLE_RATE = 44_100

/**
 * The DataStore file [name] in [filesDir], the private files of the application: `files/datastore/<name>`.
 * The backup rules (`res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml`) name these files.
 */
internal fun dataStoreFile(
    filesDir: File,
    name: String,
): Path = File(File(filesDir, DATASTORE_DIRECTORY), name).toOkioPath()

/** The directory of the DataStore files, in the private files of the application. */
private const val DATASTORE_DIRECTORY = "datastore"
