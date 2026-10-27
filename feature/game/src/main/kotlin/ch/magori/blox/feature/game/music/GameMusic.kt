package ch.magori.blox.feature.game.music

import ch.magori.blox.core.music.Music

/**
 * The music a game plays, as `app` gives it for the music chosen (docs/musique-vivante.md): it chooses the
 * [MusicDirection] of the game.
 */
sealed interface GameMusic {
    /** [music], a score or the music file of the player, played as it is: a quarter faster in danger, as the Classic. */
    data class Fixed(val music: Music) : GameMusic

    /**
     * The live music: a calm music and a tense one, composed for the game from its seed. The tense one takes over as the
     * stack enters the danger, the calm one as it leaves it; both a little faster at each level gained.
     */
    data object Live : GameMusic
}
