package ch.magori.blox.feature.game.sound

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.SoundEffects

/**
 * The sound effect [event] of the engine gives, or null for none: a rotation, lines cleared, four lines cleared at
 * once, a level up, the game over. A lock or the danger give none: a lock comes with every piece, every second or
 * so, and the music already marks the danger with its tempo.
 */
internal fun soundEffectFor(event: GameEvent): SoundEffect? = when (event) {
    is GameEvent.PieceRotated -> SoundEffects.rotation

    is GameEvent.LinesCleared -> when {
        event.lineClear.isFourLines -> SoundEffects.fourLinesCleared
        event.lineClear.count > 0 -> SoundEffects.linesCleared
        else -> null
    }

    is GameEvent.LevelUp -> SoundEffects.levelUp

    GameEvent.GameOver -> SoundEffects.gameOver

    is GameEvent.PieceLocked, is GameEvent.StackDanger -> null
}

/**
 * The sound effects the [events] of one tick give, in their order, a clear giving one effect at most. A level up
 * comes on the tick of the clear that reaches it, and the effects of a tick sound together, each on its own track:
 * two rises of the square at once would blur into one another. So the level up sound plays instead of the one to
 * three lines effect, which it also marks; four lines, the best move, keep their own effect, the longest of the
 * clears, and the level up is then only seen.
 */
internal fun soundEffectsFor(events: List<GameEvent>): List<SoundEffect> {
    val clear = events.filterIsInstance<GameEvent.LinesCleared>().firstOrNull()?.lineClear
    val levelUp = events.any { it is GameEvent.LevelUp }
    val sounding = events.filterNot { event ->
        when (event) {
            is GameEvent.LinesCleared -> levelUp && !event.lineClear.isFourLines
            is GameEvent.LevelUp -> clear?.isFourLines == true
            else -> false
        }
    }
    return sounding.mapNotNull(::soundEffectFor)
}
