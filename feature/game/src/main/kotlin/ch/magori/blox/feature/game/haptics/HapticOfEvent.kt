package ch.magori.blox.feature.game.haptics

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.haptics.Haptic

/**
 * The vibration [event] of the engine gives, or null for none: lines cleared, four lines cleared at once, the
 * game over. A lock or the danger give none: they come every few seconds, and the music already marks the danger.
 */
internal fun hapticFor(event: GameEvent): Haptic? = when (event) {
    is GameEvent.LinesCleared -> when {
        event.lineClear.isFourLines -> Haptic.FOUR_LINES_CLEARED
        event.lineClear.count > 0 -> Haptic.LINES_CLEARED
        else -> null
    }

    GameEvent.GameOver -> Haptic.GAME_OVER

    is GameEvent.PieceLocked, is GameEvent.StackDanger -> null
}
