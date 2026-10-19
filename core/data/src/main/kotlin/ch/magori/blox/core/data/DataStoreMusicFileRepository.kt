package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The file of « Mon fichier », the uri of the audio document the player chose. Apart from the settings: the backup
 * rules of the application, lists of the files to keep, leave it out, so that its uri, which names a document of the
 * player and whose permission belongs to this device, never leaves it.
 */
const val MY_MUSIC_FILE: String = "music_file.preferences_pb"

/**
 * The music file kept in [dataStore], DataStore Preferences on [MY_MUSIC_FILE]. The DataStore is created once, by
 * the application container (ADR 0007). A file that cannot be read gives no music file; a blank uri, which no version
 * writes, neither.
 */
class DataStoreMusicFileRepository(private val dataStore: DataStore<Preferences>) : MusicFileRepository {
    override val musicFile: Flow<MusicFile?> = dataStore.readOrEmpty()
        .map { stored -> stored[URI]?.takeIf { it.isNotBlank() }?.let(::MusicFile) }
        .distinctUntilChanged()

    override suspend fun setMusicFile(file: MusicFile) {
        dataStore.edit { it[URI] = file.uri }
    }

    private companion object {
        val URI = stringPreferencesKey("uri")
    }
}
