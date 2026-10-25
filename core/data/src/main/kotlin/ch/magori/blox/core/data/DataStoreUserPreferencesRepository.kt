package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.magori.blox.core.music.Volume
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
     * below 0, a volume off the steps of 10 from 0 to 100) is its default. If the file cannot be read, all
     * of them are, once, and the flow then ends.
     */
    override val preferences: Flow<UserPreferences> = dataStore.readOrEmpty().map { stored ->
        val default = UserPreferences.DEFAULT
        UserPreferences(
            theme = ThemeChoice.entries.firstOrNull { it.name == stored[THEME] } ?: default.theme,
            music = MusicChoice.entries.firstOrNull { it.name == stored[MUSIC] } ?: default.music,
            startLevel = stored[START_LEVEL]?.takeIf { it >= 0 } ?: default.startLevel,
            // Missing in the files of the versions before them: those keep their choices, with the defaults.
            vibrations = stored[VIBRATIONS] ?: default.vibrations,
            soundEffects = stored[SOUND_EFFECTS] ?: default.soundEffects,
            reducedEffects = stored[REDUCED_EFFECTS] ?: default.reducedEffects,
            leftHanded = stored[LEFT_HANDED] ?: default.leftHanded,
            keyVibration = stored[KEY_VIBRATION] ?: default.keyVibration,
            musicVolume = stored[MUSIC_VOLUME]?.let(Volume::ofPercentOrNull) ?: default.musicVolume,
            effectsVolume = stored[EFFECTS_VOLUME]?.let(Volume::ofPercentOrNull) ?: default.effectsVolume,
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

    override suspend fun setReducedEffects(reducedEffects: Boolean) {
        dataStore.edit { it[REDUCED_EFFECTS] = reducedEffects }
    }

    override suspend fun setLeftHanded(leftHanded: Boolean) {
        dataStore.edit { it[LEFT_HANDED] = leftHanded }
    }

    override suspend fun setKeyVibration(keyVibration: Boolean) {
        dataStore.edit { it[KEY_VIBRATION] = keyVibration }
    }

    override suspend fun setMusicVolume(musicVolume: Volume) {
        dataStore.edit { it[MUSIC_VOLUME] = musicVolume.percent }
    }

    override suspend fun setEffectsVolume(effectsVolume: Volume) {
        dataStore.edit { it[EFFECTS_VOLUME] = effectsVolume.percent }
    }

    private companion object {
        val START_LEVEL = intPreferencesKey("start_level")
        val THEME = stringPreferencesKey("theme")
        val MUSIC = stringPreferencesKey("music")
        val VIBRATIONS = booleanPreferencesKey("vibrations")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val REDUCED_EFFECTS = booleanPreferencesKey("reduced_effects")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val KEY_VIBRATION = booleanPreferencesKey("key_vibration")
        val MUSIC_VOLUME = intPreferencesKey("music_volume")
        val EFFECTS_VOLUME = intPreferencesKey("effects_volume")
    }
}
