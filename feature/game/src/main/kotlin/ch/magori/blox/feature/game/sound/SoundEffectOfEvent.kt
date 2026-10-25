package ch.magori.blox.feature.game.sound

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.SoundEffects

/**
 * The sound effect [event] of the engine gives, or null for none: a rotation, lines cleared, four lines cleared at
 * once, the game over. A lock or the danger give none: a lock comes with every piece, every second or so, and the
 * music already marks the danger with its tempo. A level up gives none yet (P5.69).
 */
internal fun soundEffectFor(event: GameEvent): SoundEffect? = when (event) {
    is GameEvent.PieceRotated -> SoundEffects.rotation

    is GameEvent.LinesCleared -> when {
        event.lineClear.isFourLines -> SoundEffects.fourLinesCleared
        event.lineClear.count > 0 -> SoundEffects.linesCleared
        else -> null
    }

    GameEvent.GameOver -> SoundEffects.gameOver

    is GameEvent.PieceLocked, is GameEvent.StackDanger, is GameEvent.LevelUp -> null
}
