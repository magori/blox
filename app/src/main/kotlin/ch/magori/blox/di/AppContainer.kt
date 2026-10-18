package ch.magori.blox.di

import android.util.Log
import ch.magori.blox.core.audio.AudioTrackSink
import ch.magori.blox.core.audio.ChiptuneSequencer
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Synthesizer
import ch.magori.blox.core.data.DataStoreHighScoreRepository
import ch.magori.blox.core.data.DataStoreUserPreferencesRepository
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.RECORDS_FILE
import ch.magori.blox.core.data.SETTINGS_FILE
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.data.preferencesDataStore
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * Builds what the features need, by hand (ADR 0007). The features define their view model
 * factories; this container builds them with the implementations of the application (ADR 0013).
 * It lives as long as the application, which creates it once: a DataStore file must have a single
 * instance per process.
 *
 * @param filesDir the private files directory of the application.
 */
class AppContainer(filesDir: File) {
    /** Where the DataStores live and work, for the whole life of the process. */
    private val dataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The record, in `files/datastore/records.preferences_pb`: the game and the home screen share it. */
    private val highScores: HighScoreRepository = DataStoreHighScoreRepository(
        preferencesDataStore(dataScope) { File(filesDir, "datastore/$RECORDS_FILE").toOkioPath() },
    )

    /** The settings, in `files/datastore/settings.preferences_pb`, apart from the record. */
    private val userPreferences: UserPreferencesRepository = DataStoreUserPreferencesRepository(
        preferencesDataStore(dataScope) { File(filesDir, "datastore/$SETTINGS_FILE").toOkioPath() },
    )

    /** The theme of the whole application, the one saved in the settings. */
    val appTheme: AppTheme = AppTheme(userPreferences, dataScope)

    /**
     * The music of the games: a single sequencer for the whole life of the process, rendering on
     * [Dispatchers.Default] and writing to an [AudioTrackSink] on [Dispatchers.IO]. A failure stops the
     * music, never the game; it is logged here, the only place that knows Android.
     */
    private val music: MusicPlayer = ChiptuneSequencer(
        synthesizer = Synthesizer(MUSIC_SAMPLE_RATE),
        openSink = ::AudioTrackSink,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        onFailure = { failure -> Log.w(TAG, "The music stopped", failure) },
    )

    /** Classic mode: one engine per game, from the seed of the game route (ADR 0014). */
    val gameViewModelFactory: GameViewModel.Factory =
        GameViewModel.Factory(ClassicGameEngineFactory, highScores, userPreferences, music)

    /** The settings and the record kept on the device; the start levels of the Classic mode. */
    val homeViewModelFactory: HomeViewModel.Factory =
        HomeViewModel.Factory(userPreferences, highScores, ClassicRules.levelProgression)
}

private const val TAG = "BloX"

/** Samples per second of the music: 44 100 Hz, the rate of a CD, which every Android device plays. */
private const val MUSIC_SAMPLE_RATE = 44_100
