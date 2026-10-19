package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow

/**
 * The settings of the player, chosen on the home screen (V1) and kept on the device. Each change fails
 * with an [java.io.IOException] if the settings cannot be written; any other failure is a bug.
 */
interface UserPreferencesRepository {
    /**
     * The settings; [UserPreferences.DEFAULT] until the player changes them. Emits each change once.
     *
     * Never fails with an [java.io.IOException]: settings that cannot be read give [UserPreferences.DEFAULT],
     * once, and the flow then ends. Any other failure is a bug, and goes through to the collector: it is not
     * to be caught.
     */
    val preferences: Flow<UserPreferences>

    /** @throws java.io.IOException if the settings cannot be written. */
    suspend fun setTheme(theme: ThemeChoice)

    /** @throws java.io.IOException if the settings cannot be written. */
    suspend fun setMusic(music: MusicChoice)

    /**
     * @throws java.io.IOException if the settings cannot be written.
     * @throws IllegalArgumentException if [startLevel] is negative.
     */
    suspend fun setStartLevel(startLevel: Int)
}
