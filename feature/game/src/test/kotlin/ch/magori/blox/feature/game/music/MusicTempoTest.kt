package ch.magori.blox.feature.game.music

import ch.magori.blox.core.game.engine.GameEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MusicTempoTest {
    @ParameterizedTest(name = "stack in danger: {0} -> tempo {1}")
    @CsvSource("true, 1.25", "false, 1.0")
    fun `the music speeds up by a quarter while the stack is in danger, and goes back to its tempo after`(
        on: Boolean,
        expectedFactor: Double,
    ) {
        assertThat(tempoFactorFor(GameEvent.StackDanger(on))).isEqualTo(expectedFactor)
    }
}
