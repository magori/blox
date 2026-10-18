package ch.magori.blox.feature.game.music

import ch.magori.blox.core.game.engine.GameEvent

/** The tempo of the score. */
internal const val NORMAL_TEMPO: Double = 1.0

/** The tempo while the stack is in danger: a quarter faster (docs/musique.md, Accélération). */
internal const val DANGER_TEMPO: Double = 1.25

/** The tempo factor of the music once the stack enters or leaves the [danger]. */
internal fun tempoFactorFor(danger: GameEvent.StackDanger): Double = if (danger.on) DANGER_TEMPO else NORMAL_TEMPO
