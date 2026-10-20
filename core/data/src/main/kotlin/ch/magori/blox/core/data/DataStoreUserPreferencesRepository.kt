package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The settings kept in [dataStore], DataStore Preferences on [SETTINGS_FILE], apart from the record.
 * The DataStore is created once, by the application container (ADR 0007). Each setting is stored by
 * its name, so that adding a choice keeps the saved ones.
 */
class DataStoreUserPreferencesRepository(private val dataStore: DataStore<Preferences>) : UserPreferencesRepository {
    /**
     * The settings; each one missing or unknown in the file (a value a later version removed, a level
     * below 0) is its default. If the file cannot be read, all of them are, once, and the flow then ends.
     */
    override val preferences: Flow<UserPreferences> = dataStore.readOrEmpty().map { stored ->
        UserPreferences(
            theme = ThemeChoice.entries.firstOrNull { it.name == stored[THEME] } ?: UserPreferences.DEFAULT.theme,
            music = MusicChoice.entries.firstOrNull { it.name == stored[MUSIC] } ?: UserPreferences.DEFAULT.music,
            startLevel = stored[START_LEVEL]?.takeIf { it >= 0 }
                ?: UserPreferences.DEFAULT.startLevel,
            // Missing in the files of the versions before them: those keep their choices, with the defaults.
            vibrations = stored[VIBRATIONS] ?: UserPreferences.DEFAULT.vibrations,
            soundEffects = stored[SOUND_EFFECTS] ?: UserPreferences.DEFAULT.soundEffects,
        )
    }.distinctUntilChanged()

    override suspend fun setTheme(theme: ThemeChoice) {
        dataStore.edit { it[THEME] = theme.name }
    }

    override suspend fun setMusic(music: MusicChoice) {
        dataStore.edit { it[MUSIC] = music.name }
    }

    override suspend fun setStartLevel(startLevel: Int) {
        require(startLevel >= 0) { "A start level cannot be negative: $startLevel" }
        dataStore.edit { it[START_LEVEL] = startLevel }
    }

    override suspend fun setVibrations(vibrations: Boolean) {
        dataStore.edit { it[VIBRATIONS] = vibrations }
    }

    override suspend fun setSoundEffects(soundEffects: Boolean) {
        dataStore.edit { it[SOUND_EFFECTS] = soundEffects }
    }

    private companion object {
        val START_LEVEL = intPreferencesKey("start_level")
        val THEME = stringPreferencesKey("theme")
        val MUSIC = stringPreferencesKey("music")
        val VIBRATIONS = booleanPreferencesKey("vibrations")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
    }
}
