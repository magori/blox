package ch.magori.blox.feature.game.music

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.music.Scores
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class MusicScoresTest {
    @ParameterizedTest
    @MethodSource("choices")
    fun `each music choice plays its score, or none`(
        choice: MusicChoice,
        expected: Any?,
    ) {
        assertThat(scoreFor(choice)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "stack in danger: {0} -> tempo {1}")
    @CsvSource("true, 1.25", "false, 1.0")
    fun `the music speeds up by a quarter while the stack is in danger, and goes back to its tempo after`(
        on: Boolean,
        expectedFactor: Double,
    ) {
        assertThat(tempoFactorFor(GameEvent.StackDanger(on))).isEqualTo(expectedFactor)
    }

    private companion object {
        @JvmStatic
        fun choices(): List<Arguments> = listOf(
            argumentSet("Korobeiniki", MusicChoice.KOROBEINIKI, Scores.korobeiniki),
            argumentSet("the minuet", MusicChoice.MINUET, Scores.minuet),
            argumentSet("no music", MusicChoice.NONE, null),
            argumentSet("my file, not played yet (P4.6)", MusicChoice.MY_FILE, null),
        )
    }
}
