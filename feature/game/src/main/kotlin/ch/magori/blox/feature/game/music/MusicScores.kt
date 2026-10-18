package ch.magori.blox.feature.game.music

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Scores

/** The score played during a game for the music [choice] of the player; null when none is played. */
internal fun scoreFor(choice: MusicChoice): Score? = when (choice) {
    MusicChoice.KOROBEINIKI -> Scores.korobeiniki

    MusicChoice.MINUET -> Scores.minuet

    MusicChoice.NONE -> null

    // The file of the player comes with its own player (P4.6): no music until then.
    MusicChoice.MY_FILE -> null
}

/** The tempo of the score. */
internal const val NORMAL_TEMPO: Double = 1.0

/** The tempo while the stack is in danger: a quarter faster (docs/musique.md, Accélération). */
internal const val DANGER_TEMPO: Double = 1.25

/** The tempo factor of the music once the stack enters or leaves the [danger]. */
internal fun tempoFactorFor(danger: GameEvent.StackDanger): Double = if (danger.on) DANGER_TEMPO else NORMAL_TEMPO
