package ch.magori.blox.core.data

import ch.magori.blox.core.music.LiveMusic
import ch.magori.blox.core.music.Music

/**
 * What a [MusicChoice] asks a game to play: a [Fixed] music, the same from one game to the next, or the [Live] music,
 * composed for each game. Sealed, so that the game treats each one on purpose; the application maps the choices to it.
 */
sealed interface ChosenMusic {
    /** A [music] played as it is, a BloX score or the music file of the player, whatever the game. */
    data class Fixed(val music: Music) : ChosenMusic

    /**
     * The live music: its two moods, a calm music and a tense one, are composed for each game from the seed of the game
     * ([LiveMusic.compose]), and not by the choice.
     */
    data object Live : ChosenMusic
}
