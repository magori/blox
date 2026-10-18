package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path

/** The file of the records, apart from the settings (P3.1). */
const val RECORDS_FILE: String = "records.preferences_pb"

/**
 * The DataStore of the records, living in [scope], on the file [produceFile] gives. The storage is
 * Okio's, in production as in the tests: `createWithPath` falls back to the plain file storage on
 * Android, which cannot replace the file on Windows.
 */
fun recordsDataStore(
    scope: CoroutineScope,
    produceFile: () -> Path,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer, producePath = produceFile),
    // An unreadable file is replaced by an empty one: the record starts again from 0.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope,
)
