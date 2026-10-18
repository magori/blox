package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow

/** The settings of the player, chosen on the home screen (V1) and kept on the device. */
interface UserPreferencesRepository {
    /** The settings; [UserPreferences.DEFAULT] until the player changes them. Emits each change once. */
    val preferences: Flow<UserPreferences>

    suspend fun setTheme(theme: ThemeChoice)

    suspend fun setMusic(music: MusicChoice)

    /** @throws IllegalArgumentException if [startLevel] is negative. */
    suspend fun setStartLevel(startLevel: Int)
}
