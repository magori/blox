package ch.magori.blox.feature.game.music

import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.game.engine.GameEvent

/**
 * The music of the Classic: a quarter faster while the stack is in danger (docs/musique.md, Accélération). One
 * instance per game: it follows the danger of its game, to answer for it when the music starts late. [inDanger] is
 * where the game stands when its music is known: a music read late may come once the stack is in danger.
 */
internal class ClassicMusicDirection(inDanger: Boolean = false) : MusicDirection {
    /** Whether the stack of the game is in danger, as its last change of danger gave it. */
    private var inDanger: Boolean = inDanger

    /**
     * Prepares the danger tempo at once: entering the danger, the most tense moment of the game, then renders
     * nothing. A game already in danger gives the player its tempo: the music just played is at the one of its score.
     */
    override fun onStart(player: MusicPlayer) {
        player.prepareTempo(DANGER_TEMPO_FACTOR)
        if (inDanger) player.setTempoFactor(DANGER_TEMPO_FACTOR)
    }

    /** Each change of danger gives the [player] its tempo ([tempoFactorFor]); the other events change nothing. */
    override fun onEvents(
        events: List<GameEvent>,
        player: MusicPlayer,
    ) {
        events.filterIsInstance<GameEvent.StackDanger>().forEach {
            inDanger = it.on
            player.setTempoFactor(tempoFactorFor(it))
        }
    }

    companion object {
        /** The tempo of the score. */
        private const val NORMAL_TEMPO: Double = 1.0

        /**
         * The tempo factor of the music once the stack enters or leaves the [danger]: the rule itself, fixed by its
         * historical test; [onEvents] is the way in for the game.
         */
        fun tempoFactorFor(danger: GameEvent.StackDanger): Double = if (danger.on) DANGER_TEMPO_FACTOR else NORMAL_TEMPO
    }
}
