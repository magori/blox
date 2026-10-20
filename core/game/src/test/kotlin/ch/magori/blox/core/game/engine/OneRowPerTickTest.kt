package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * The double of the instrumented tests that need a game over ([OneRowPerTickGameEngineFactory]): what it promises
 * them, so that their wait for the game over screen (a minute of the clock, 3 600 ticks) has a reason.
 */
class OneRowPerTickTest {
    @Test
    fun `the piece falls one row per tick, at the slowest level as at the fastest`() {
        listOf(0, 9).forEach { level ->
            // Given
            val engine = OneRowPerTickGameEngineFactory.create(seed = 1L)
            val started = engine.start(Level(level)).state
            val row = checkNotNull(started.current).pivot.row

            // When
            val ticked = engine.tick(started).state

            // Then
            assertThat(checkNotNull(ticked.current).pivot.row).describedAs("level $level").isEqualTo(row + 1)
        }
    }

    @ParameterizedTest
    @ValueSource(longs = [1L, 2L, 3L, 42L, -7L])
    fun `a game left without any input is over in fewer than 1 200 ticks, a third of the wait of the tests`(
        seed: Long,
    ) {
        // Given
        val engine = OneRowPerTickGameEngineFactory.create(seed)
        var state = engine.start(Level(0)).state

        // When
        while (state.phase !is Phase.GameOver && state.tickCount < MAX_TICKS) state = engine.tick(state).state

        // Then
        assertThat(state.phase).isInstanceOf(Phase.GameOver::class.java)
        assertThat(state.tickCount).isLessThan(MAX_TICKS)
    }

    private companion object {
        /** 20 s of the clock: a third of the minute the instrumented tests wait for the game over screen. */
        const val MAX_TICKS = 1_200
    }
}
