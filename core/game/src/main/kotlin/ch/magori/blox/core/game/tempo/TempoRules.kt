package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.Rules

/**
 * The rules of Au tempo (ADR 0028): the rules of the Classic ([ClassicRules]), but for the drop timing, the piece
 * falling one row on every eighth of [meter] ([TempoDropTiming]), whatever the level. The tempo is fixed for the game
 * by [meter]; the one of a game is chosen by its caller, not here.
 *
 * The window on the beat of [meter] ([Meter.onBeatWindow]) is not read by the engine but by [TempoScore]: a game passes
 * the same [meter] to both.
 */
public fun tempoRules(meter: Meter): Rules = ClassicRules.copy(dropTiming = TempoDropTiming(meter))
