package ch.magori.blox.core.data

/** The theme chosen on the home screen (V1). The feature maps it to its theme. */
enum class ThemeChoice { ORIGINAL, COLORFUL }

/** The music chosen on the home screen (V1). The application maps it to its scores or the file player. */
enum class MusicChoice { KOROBEINIKI, MINUET, MY_FILE, NONE }

/**
 * The settings of the player, chosen on the home screen (V1). [startLevel] is never negative; the
 * levels a game may start at belong to the game mode, which checks them: the home screen offers only
 * those. [vibrations] says whether the game vibrates when lines are cleared and when it is over.
 */
data class UserPreferences(
    val theme: ThemeChoice,
    val music: MusicChoice,
    val startLevel: Int,
    val vibrations: Boolean,
) {
    companion object {
        /**
         * The settings until the player changes them: the Original theme, Korobeiniki, level 0, vibrations off.
         * The vibrations stay off as long as no screen lets the player turn them off again: before Android 13, no
         * setting of the system would. The task that adds that switch settles their default for good.
         */
        val DEFAULT: UserPreferences =
            UserPreferences(ThemeChoice.ORIGINAL, MusicChoice.KOROBEINIKI, startLevel = 0, vibrations = false)
    }
}
