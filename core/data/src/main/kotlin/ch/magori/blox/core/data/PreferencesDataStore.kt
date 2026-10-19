package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import okio.FileSystem
import okio.Path
import java.io.IOException

/** The file of the records. Apart from the settings: resetting the settings keeps the record. */
const val RECORDS_FILE: String = "records.preferences_pb"

/** The file of the settings (theme, music, start level). Apart from the records. */
const val SETTINGS_FILE: String = "settings.preferences_pb"

/**
 * A DataStore Preferences living in [scope], on the file [produceFile] gives ([RECORDS_FILE],
 * [SETTINGS_FILE]); one instance per file. The storage is Okio's, in production as in the tests:
 * `createWithPath` falls back to the plain file storage on Android, which cannot replace the file
 * on Windows. An unreadable file is replaced by an empty one: its values start again from their
 * defaults.
 */
fun preferencesDataStore(
    scope: CoroutineScope,
    produceFile: () -> Path,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer, producePath = produceFile),
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope,
)

/**
 * The preferences of [this] DataStore. If the file cannot be read (an [IOException]): empty preferences,
 * once, and the flow then ends, the contract of the repositories. Any other failure is a bug and goes through.
 */
internal fun DataStore<Preferences>.readOrEmpty(): Flow<Preferences> = readOr(emptyPreferences())
