package ch.magori.blox.core.data

import ch.magori.blox.core.music.SoundStyle
import ch.magori.blox.core.music.Volume

/** The theme chosen on the home screen (V1). The feature maps it to its theme. */
enum class ThemeChoice { ORIGINAL, COLORFUL }

/**
 * The music chosen on the home screen (V1). The application maps it to its scores, the file player, or for [LIVE] to
 * a live music, composed for each game from its seed. Each choice is saved by its name, which must not change.
 */
enum class MusicChoice {
    KOROBEINIKI,
    MINUET,
    MY_FILE,
    LIVE,
    NONE,
    ;

    companion object {
        /**
         * What [MY_FILE] plays when it has no file to play: none kept (the settings restored on another device, the
         * file never leaving this one), or a file that can no longer be read. The game plays it; the home screen names
         * it in place of [MY_FILE] when it knows there is no file. It must be a music with a BloX score (Korobeïniki or
         * the minuet): the application reads the score of it once, when it starts, and stops if there is none.
         */
        val MY_FILE_FALLBACK: MusicChoice = KOROBEINIKI
    }
}

/**
 * The settings of the player, chosen on the home screen (V1) and in the settings. [startLevel] is never
 * negative; the levels a game may start at belong to the game mode, which checks them: the home screen offers
 * only those. [vibrations] says whether the game vibrates when lines are cleared and when it is over;
 * [soundEffects] whether it plays its short sound effects (a rotation, lines cleared, the game over).
 * [reducedEffects] asks for fewer visual effects; [leftHanded] for the controls laid out for the left hand;
 * [keyVibration] for a short vibration on each key pressed. [musicVolume] and [effectsVolume] are the volumes
 * of the music and of the sound effects; [soundStyle] the instruments of every score of BloX.
 */
data class UserPreferences(
    val theme: ThemeChoice,
    val music: MusicChoice,
    val startLevel: Int,
    val vibrations: Boolean,
    val soundEffects: Boolean,
    val reducedEffects: Boolean,
    val leftHanded: Boolean,
    val keyVibration: Boolean,
    val musicVolume: Volume,
    val effectsVolume: Volume,
    val soundStyle: SoundStyle,
) {
    companion object {
        /**
         * The settings until the player changes them: the Original theme, Korobeiniki, level 0, vibrations off,
         * sound effects on, effects not reduced, the controls for the right hand, keys that do not vibrate, both
         * volumes full, the Arcade instruments. The vibrations are off until the player turns them on; the sound
         * effects are on until the player turns them off: the choices of the developer.
         */
        val DEFAULT: UserPreferences = UserPreferences(
            ThemeChoice.ORIGINAL,
            MusicChoice.KOROBEINIKI,
            startLevel = 0,
            vibrations = false,
            soundEffects = true,
            reducedEffects = false,
            leftHanded = false,
            keyVibration = false,
            musicVolume = Volume.FULL,
            effectsVolume = Volume.FULL,
            soundStyle = SoundStyle.ARCADE,
        )
    }
}
