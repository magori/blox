package ch.magori.blox.feature.game.music

import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.game.engine.GameEvent

/**
 * What the music of a game does as the game goes (docs/musique-vivante.md, Diriger la musique pendant la partie):
 * the translation of the events of the game into orders to the player. Chosen at the start of the game, after its
 * music.
 */
internal interface MusicDirection {
    /**
     * The music of the game starts from its start on [player], which has just been told to play it. It may come after
     * [onEvents] (the music starts late, or plays again on a resume): the direction then answers for its own state,
     * which the game may have changed since.
     */
    fun onStart(player: MusicPlayer)

    /**
     * The game gave [events] on one tick: the orders they call for go to [player], even while it plays nothing, which
     * keeps them for the resume.
     */
    fun onEvents(
        events: List<GameEvent>,
        player: MusicPlayer,
    )
}
