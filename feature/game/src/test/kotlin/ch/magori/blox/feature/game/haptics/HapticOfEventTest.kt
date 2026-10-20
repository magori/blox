package ch.magori.blox.feature.game.haptics

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.spawned
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.haptics.Haptic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HapticOfEventTest {
    @ParameterizedTest(name = "{0} lines")
    @ValueSource(ints = [1, 2, 3])
    fun `one to three lines cleared give the short pulse`(count: Int) {
        // When
        val haptic = hapticFor(GameEvent.LinesCleared(LineClear(count)))

        // Then
        assertThat(haptic).isEqualTo(Haptic.LINES_CLEARED)
    }

    @Test
    fun `four lines cleared at once give the stronger pulse`() {
        // When
        val haptic = hapticFor(GameEvent.LinesCleared(LineClear(4)))

        // Then
        assertThat(haptic).isEqualTo(Haptic.FOUR_LINES_CLEARED)
    }

    @Test
    fun `the game over gives its own pattern`() {
        // When
        val haptic = hapticFor(GameEvent.GameOver)

        // Then
        assertThat(haptic).isEqualTo(Haptic.GAME_OVER)
    }

    @Test
    fun `a rotation gives nothing`() {
        // When
        val haptic = hapticFor(GameEvent.PieceRotated(spawned(PieceType.T)))

        // Then
        assertThat(haptic).isNull()
    }

    @Test
    fun `a clear of no line, a lock and the danger give nothing`() {
        // Given
        val events = listOf(
            GameEvent.LinesCleared(LineClear(0)),
            GameEvent.PieceLocked(spawned(PieceType.O)),
            GameEvent.StackDanger(on = true),
            GameEvent.StackDanger(on = false),
        )

        // When
        val haptics = events.map(::hapticFor)

        // Then
        assertThat(haptics).containsOnlyNulls()
    }
}
