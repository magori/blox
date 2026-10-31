package ch.magori.blox.core.data

import ch.magori.blox.core.music.SoundStyle
import ch.magori.blox.core.music.Volume
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

    /**
     * Saves whether the game vibrates on the lines cleared and at the game over.
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setVibrations(vibrations: Boolean)

    /**
     * Saves whether the game plays its sound effects.
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setSoundEffects(soundEffects: Boolean)

    /**
     * Saves whether the game shows fewer visual effects.
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setReducedEffects(reducedEffects: Boolean)

    /**
     * Saves whether the controls are laid out for the left hand.
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setLeftHanded(leftHanded: Boolean)

    /**
     * Saves whether each key pressed vibrates.
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setKeyVibration(keyVibration: Boolean)

    /**
     * Saves the volume of the music.
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setMusicVolume(musicVolume: Volume)

    /**
     * Saves the volume of the sound effects.
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setEffectsVolume(effectsVolume: Volume)

    /**
     * Saves the instruments of the scores, « Instruments ».
     *
     * @throws java.io.IOException if the settings cannot be written.
     */
    suspend fun setSoundStyle(soundStyle: SoundStyle)
}
