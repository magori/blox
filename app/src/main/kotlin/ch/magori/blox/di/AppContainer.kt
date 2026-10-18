package ch.magori.blox.di

import ch.magori.blox.core.data.DataStoreHighScoreRepository
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.RECORDS_FILE
import ch.magori.blox.core.data.preferencesDataStore
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.feature.game.GameViewModel
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

    /** The record, in `files/datastore/records.preferences_pb`. */
    private val highScores: HighScoreRepository = DataStoreHighScoreRepository(
        preferencesDataStore(dataScope) { File(filesDir, "datastore/$RECORDS_FILE").toOkioPath() },
    )

    /** Classic mode: one engine per game, from the seed of the game route (ADR 0014). */
    val gameViewModelFactory: GameViewModel.Factory = GameViewModel.Factory(ClassicGameEngineFactory, highScores)
}
