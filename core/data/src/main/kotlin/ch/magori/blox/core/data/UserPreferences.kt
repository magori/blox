package ch.magori.blox.core.data

/** The theme chosen on the home screen (V1). The feature maps it to its theme. */
enum class ThemeChoice { ORIGINAL, COLORFUL }

/** The music chosen on the home screen (V1). The feature maps it to its scores or the file player. */
enum class MusicChoice { KOROBEINIKI, MINUET, MY_FILE, NONE }

/**
 * The settings of the player, chosen on the home screen (V1). [startLevel] is never negative; the
 * levels a game may start at belong to the game mode, which checks them: the home screen offers only
 * those.
 */
data class UserPreferences(val theme: ThemeChoice, val music: MusicChoice, val startLevel: Int) {
    companion object {
        /** The settings until the player changes them: the Original theme, Korobeiniki, level 0. */
        val DEFAULT: UserPreferences = UserPreferences(ThemeChoice.ORIGINAL, MusicChoice.KOROBEINIKI, startLevel = 0)
    }
}
