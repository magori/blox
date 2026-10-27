package ch.magori.blox.feature.game.music

import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.music.LiveMusic
import ch.magori.blox.core.music.Score

/**
 * The live music of a game started at [startLevel] (docs/musique-vivante.md, Diriger la musique pendant la partie): the
 * calm music of [live] at the start, the tense one while the stack is in danger, each taking over from the other at
 * the next bar ([MusicPlayer.switchTo]); a little faster at each level gained, and a quarter faster again in danger.
 * The game plays the calm music, then lets the direction start ([onStart]). One instance per game: it follows the
 * danger and the level of its game, to answer for them when the music starts late. [level] and [inDanger] are where
 * the game stands when its music is known: a music composed late may come after a level up, or once in danger.
 */
internal class LiveMusicDirection(
    private val live: LiveMusic,
    private val startLevel: Level,
    level: Level = startLevel,
    inDanger: Boolean = false,
) : MusicDirection {
    /** The level of the game, as its last level up gave it. */
    private var level: Level = level

    /** Whether the stack of the game is in danger, as its last change of danger gave it. */
    private var inDanger: Boolean = inDanger

    /**
     * The calm music was just played, at the tempo of its score: a game already in danger gives the player its tempo
     * and the tense music, which then plays from its start; a game already gone up a level, its tempo.
     */
    override fun onStart(player: MusicPlayer) {
        if (inDanger) {
            changeMood(player)
        } else if (level != startLevel) {
            player.setTempoFactor(tempoFactor())
        }
    }

    /**
     * The changes of danger and the level ups of the tick are all applied first; then the player is given the tempo of
     * the moment if it changed, and the music of the new mood if the mood changed, the tempo first: of a level up and
     * the end of the danger on the same tick, the music is rendered once. Each level up below the cap renders the music
     * again. The other events change nothing.
     */
    override fun onEvents(
        events: List<GameEvent>,
        player: MusicPlayer,
    ) {
        val tempoBefore = tempoFactor()
        val dangerBefore = inDanger
        events.forEach { event ->
            when (event) {
                is GameEvent.StackDanger -> inDanger = event.on
                is GameEvent.LevelUp -> level = event.level
                else -> Unit
            }
        }
        val tempo = tempoFactor()
        if (tempo != tempoBefore) player.setTempoFactor(tempo)
        if (inDanger != dangerBefore) player.switchTo(mood())
    }

    /**
     * Gives [player] the music of the mood of the moment, at its tempo, the tempo first: the music switched to is
     * rendered at that tempo only, never at the one before.
     */
    private fun changeMood(player: MusicPlayer) {
        player.setTempoFactor(tempoFactor())
        player.switchTo(mood())
    }

    /** The music of the mood of the moment: the tense one in danger, the calm one otherwise. */
    private fun mood(): Score = if (inDanger) live.tense.score else live.calm.score

    /**
     * The tempo factor of the moment: [LEVEL_SPEED_UP] more for each level gained since the start, added and not
     * compounded, up to [MAX_LEVEL_TEMPO], then times [DANGER_TEMPO_FACTOR] in danger.
     */
    private fun tempoFactor(): Double {
        val levels = minOf(1 + LEVEL_SPEED_UP * (level.value - startLevel.value), MAX_LEVEL_TEMPO)
        return if (inDanger) levels * DANGER_TEMPO_FACTOR else levels
    }

    private companion object {
        /**
         * How much faster each level gained makes the music: 2 percent of its tempo. To be checked by playing (marked
         * « à vérifier » in docs/musique-vivante.md).
         */
        const val LEVEL_SPEED_UP: Double = 0.02

        /** The most the levels speed the music up, before the danger: 30 percent. To be checked by playing, as well. */
        const val MAX_LEVEL_TEMPO: Double = 1.30
    }
}
