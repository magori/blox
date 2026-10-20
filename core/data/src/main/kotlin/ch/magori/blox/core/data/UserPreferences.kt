package ch.magori.blox.core.data

/** The theme chosen on the home screen (V1). The feature maps it to its theme. */
enum class ThemeChoice { ORIGINAL, COLORFUL }

/** The music chosen on the home screen (V1). The application maps it to its scores or the file player. */
enum class MusicChoice { KOROBEINIKI, MINUET, MY_FILE, NONE }

/**
 * The settings of the player, chosen on the home screen (V1). [startLevel] is never negative; the
 * levels a game may start at belong to the game mode, which checks them: the home screen offers only
 * those. [vibrations] says whether the game vibrates when lines are cleared and when it is over;
 * [soundEffects] whether it plays its short sound effects (a rotation, lines cleared, the game over).
 */
data class UserPreferences(
    val theme: ThemeChoice,
    val music: MusicChoice,
    val startLevel: Int,
    val vibrations: Boolean,
    val soundEffects: Boolean,
) {
    companion object {
        /**
         * The settings until the player changes them: the Original theme, Korobeiniki, level 0, vibrations off,
         * sound effects off. The vibrations and the sound effects stay off as long as no screen lets the player turn
         * them off again: before Android 13, no setting of the system would turn the vibrations off, and the sound
         * effects are the choice of the developer. The tasks that add their switches settle their defaults for good.
         */
        val DEFAULT: UserPreferences = UserPreferences(
            ThemeChoice.ORIGINAL,
            MusicChoice.KOROBEINIKI,
            startLevel = 0,
            vibrations = false,
            soundEffects = false,
        )
    }
}
