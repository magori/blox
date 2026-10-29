package ch.magori.blox.di

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import ch.magori.blox.R
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
import ch.magori.blox.core.data.DAILY_CHALLENGE_FILE
import ch.magori.blox.core.data.DailyChallengeRepository
import ch.magori.blox.core.data.DataStoreDailyChallengeRepository
import ch.magori.blox.core.data.DataStoreGardenLibrary
import ch.magori.blox.core.data.DataStoreGardenRepository
import ch.magori.blox.core.data.DataStoreHighScoreRepository
import ch.magori.blox.core.data.DataStoreMusicFileRepository
import ch.magori.blox.core.data.DataStoreMusicLibrary
import ch.magori.blox.core.data.DataStorePadlockProgressRepository
import ch.magori.blox.core.data.DataStoreUserPreferencesRepository
import ch.magori.blox.core.data.GARDENS_FILE
import ch.magori.blox.core.data.GARDEN_FILE
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MUSICS_FILE
import ch.magori.blox.core.data.MY_MUSIC_FILE
import ch.magori.blox.core.data.MusicFileRepository
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.PADLOCKS_FILE
import ch.magori.blox.core.data.PadlockProgressRepository
import ch.magori.blox.core.data.RECORDS_FILE
import ch.magori.blox.core.data.SETTINGS_FILE
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.data.preferencesDataStore
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.haptics.Haptics
import ch.magori.blox.core.haptics.SystemHaptics
import ch.magori.blox.daily.DailyScoreCopier
import ch.magori.blox.daily.DailyScoreTexts
import ch.magori.blox.daily.DailySeed
import ch.magori.blox.daily.NewCartridges
import ch.magori.blox.daily.today
import ch.magori.blox.feature.composer.ComposerViewModel
import ch.magori.blox.feature.daily.DailyViewModel
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.garden.GardenViewModel
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.SettingsViewModel
import ch.magori.blox.feature.locksmith.LocksmithViewModel
import ch.magori.blox.feature.musics.DeletedMusicRelay
import ch.magori.blox.feature.musics.MyMusicsViewModel
import ch.magori.blox.locksmith.PadlockFrontier
import ch.magori.blox.music.FALLBACK_SCORE
import ch.magori.blox.music.applySavedVolumes
import ch.magori.blox.music.gameMusicScore
import ch.magori.blox.music.musicOf
import ch.magori.blox.music.scoreOf
import ch.magori.blox.theme.AndroidAnimationSetting
import ch.magori.blox.theme.AppTheme
import ch.magori.blox.theme.SystemAnimations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import java.time.Clock

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

    /**
     * Where the work of the players on the main thread, their thread, lives: the volumes saved applied to them; and the
     * writes of the daily games, of the garden and of the padlocks opened, asked for there at once, in their order. For
     * the whole life of the process, as [dataScope] and the players: it is never cancelled.
     */
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The record, in `files/datastore/records.preferences_pb`: the game and the home screen share it. */
    private val highScores: HighScoreRepository = DataStoreHighScoreRepository(
        preferencesDataStore(dataScope) { dataStoreFile(filesDir, RECORDS_FILE) },
    )

    /**
     * The results of the daily game, in `files/datastore/daily_challenge.preferences_pb`, apart from the record: the
     * backup rules leave it out, so that a first try never comes back on another device.
     */
    private val dailyChallenge: DailyChallengeRepository = DataStoreDailyChallengeRepository(
        preferencesDataStore(dataScope) { dataStoreFile(filesDir, DAILY_CHALLENGE_FILE) },
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
     * list « Mes musiques » shows and changes them all; the editor will receive it (P4.19d). Its changes are dated by
     * the clock of the system, the only one the application reads.
     */
    val musicLibrary: MusicLibrary =
        DataStoreMusicLibrary(dataScope, Clock.systemUTC()) { dataStoreFile(filesDir, MUSICS_FILE) }

    /**
     * The last garden of the Jardin musical, its pieces and its mood, in `files/datastore/garden.bin`: a creation of the
     * player, which the backup rules keep, as the musics. Built here, once for the process; the screen of the garden
     * (`feature:garden`) reads it when it opens and writes it at each change.
     */
    internal val keptGarden: GardenRepository =
        DataStoreGardenRepository(dataScope) { dataStoreFile(filesDir, GARDEN_FILE) }

    /**
     * The gardens the player kept by name, « Mes jardins », in `files/datastore/gardens.bin`, which the backup rules keep
     * too. Built here, once for the process; the sheet « Mes jardins » of the garden shows and changes them. Its changes
     * are dated by the clock of the system, as the musics.
     */
    internal val gardenLibrary: GardenLibrary =
        DataStoreGardenLibrary(dataScope, Clock.systemUTC()) { dataStoreFile(filesDir, GARDENS_FILE) }

    /**
     * The progress of the Serrurier, the best opening of each padlock opened, in `files/datastore/padlocks.bin`, which
     * the backup rules keep: what the player earned. Built here, once for the process; the screen of a padlock writes
     * its opening there.
     */
    private val padlockProgress: PadlockProgressRepository =
        DataStorePadlockProgressRepository(dataScope) { dataStoreFile(filesDir, PADLOCKS_FILE) }

    /** The theme of the whole application, the one saved in the settings. */
    val appTheme: AppTheme = AppTheme(userPreferences, dataScope)

    /** Whether Android animates, observed for the whole life of the process: when it does not, effects are reduced. */
    val systemAnimations: SystemAnimations = SystemAnimations(AndroidAnimationSetting(context), dataScope)

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

    init {
        // The volumes saved, applied to the music and to the effects shared by every screen, from the start and at each
        // change: a preview, a game, hear the slider of the settings at once.
        mainScope.applySavedVolumes(userPreferences, music = music, effects = soundEffects)
    }

    /**
     * Classic mode: one engine per game, from the seed of the game route (ADR 0014); the music of the player chosen for
     * the games, else the one chosen on the home screen; the vibrations; the sound effects. The daily game: its results
     * (ADR 0026), asked for on the main thread for the whole life of the process: a game left before its end still
     * gives its points once its view model is cleared, and the writes of the games keep their order (the DataStore
     * does its work on its own scope).
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
        dailyChallenge,
        mainScope,
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

    /**
     * The settings page: the settings kept on the device, read by each game when it is created; the record, which it
     * clears; the sound effects and the vibrations of the games, for a setting turned on to be felt once.
     */
    val settingsViewModelFactory: SettingsViewModel.Factory =
        SettingsViewModel.Factory(userPreferences, highScores, soundEffects, haptics)

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

    /**
     * Builds the view model of the Locksmith (ADR 0025): its sound effects, as the settings want them; the progress of
     * the Serrurier, where each opening is written in [mainScope], so that leaving the padlock at once does not lose it.
     */
    val locksmithViewModelFactory: LocksmithViewModel.Factory =
        LocksmithViewModel.Factory(soundEffects, userPreferences, padlockProgress, mainScope)

    /**
     * The entry of the Graine du jour (V38): its results; the level imposed on its game, for the reminder under Jouer;
     * the local date of the phone, read anew at each showing ([today], as its game), and the seed of that date, the one
     * its game is given (DailySeed), so that the seven first pieces shown are those of the game.
     */
    val dailyViewModelFactory: DailyViewModel.Factory =
        DailyViewModel.Factory(dailyChallenge, GameRoute.DAILY_START_LEVEL, today = { today() }, DailySeed::of)

    /**
     * Builds the view model of the musical garden (ADR 0027), on the garden kept: written in [mainScope], at once and in
     * the order of the changes, even when the garden is left just after one; its score played by the music player of the
     * games and of the previews, one music at a time, its preview heard by the sound effects, as the settings want them;
     * the gardens kept by name in [gardenLibrary].
     */
    val gardenViewModelFactory: GardenViewModel.Factory =
        GardenViewModel.Factory(keptGarden, gardenLibrary, mainScope, music, soundEffects, userPreferences)

    /**
     * « Copier mon score » of the Graine du jour, on its entry and at the game over of its game: the text of the results
     * of the day, in the texts of the application, put in the clipboard of the system; below Android 13, which shows
     * nothing of a copy, a toast says it. On the main thread, for the whole life of the process.
     */
    internal val dailyScoreCopier: DailyScoreCopier = DailyScoreCopier(
        results = dailyChallenge.results,
        texts = DailyScoreTexts(
            score = { seed, firstTry, best, tries ->
                context.getString(R.string.daily_score_text, seed, firstTry, best, tries)
            },
            tries = { count, written ->
                context.resources.getQuantityString(R.plurals.daily_score_tries, count, written)
            },
        ),
        clipboard = { text ->
            val clipboard = checkNotNull(context.getSystemService(ClipboardManager::class.java))
            clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
        },
        confirm = { Toast.makeText(context, R.string.daily_score_copied, Toast.LENGTH_SHORT).show() },
        sdkInt = Build.VERSION.SDK_INT,
        scope = mainScope,
    )

    /**
     * The cartridges that bear the sticker NOUVELLE on the choice of the game: the Graine du jour, until the first try of
     * the local date of the phone ([today], read anew each time the cartridges show), from its results, read once and
     * kept warm for the whole life of the process, as the DataStore.
     */
    val newCartridges: NewCartridges = NewCartridges(dailyChallenge.results, dataScope) { today() }

    /**
     * The frontier of the Serrurier, the first padlock not open: the one its cartridge opens and names. From the progress
     * of the padlocks, read once and kept warm for the whole life of the process, as the DataStore: up to date with
     * each padlock opened, which the screen of a padlock writes there.
     */
    val padlockFrontier: PadlockFrontier = PadlockFrontier(padlockProgress.progress, dataScope)
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
