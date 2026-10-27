package ch.magori.blox.feature.game.music

import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.game.engine.GameEvent

/** The music of the Classic: a quarter faster while the stack is in danger (docs/musique.md, Accélération). */
internal object ClassicMusicDirection : MusicDirection {
    /** The tempo of the score. */
    private const val NORMAL_TEMPO: Double = 1.0

    /** The tempo while the stack is in danger: a quarter faster. */
    private const val DANGER_TEMPO: Double = 1.25

    /**
     * Prepares the danger tempo at once: entering the danger, the most tense moment of the game, then renders
     * nothing.
     */
    override fun onStart(player: MusicPlayer) {
        player.prepareTempo(DANGER_TEMPO)
    }

    /** Each change of danger gives the [player] its tempo ([tempoFactorFor]); the other events change nothing. */
    override fun onEvents(
        events: List<GameEvent>,
        player: MusicPlayer,
    ) {
        events.filterIsInstance<GameEvent.StackDanger>().forEach { player.setTempoFactor(tempoFactorFor(it)) }
    }

    /**
     * The tempo factor of the music once the stack enters or leaves the [danger]: the rule itself, fixed by its
     * historical test; [onEvents] is the way in for the game.
     */
    fun tempoFactorFor(danger: GameEvent.StackDanger): Double = if (danger.on) DANGER_TEMPO else NORMAL_TEMPO
}
