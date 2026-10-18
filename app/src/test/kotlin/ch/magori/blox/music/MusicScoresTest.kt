package ch.magori.blox.music

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Scores
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class MusicScoresTest {
    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("choices")
    fun `each music choice plays its score, or none`(
        choice: MusicChoice,
        expected: Score?,
    ) {
        // Given / When / Then
        assertThat(scoreOf(choice)).isEqualTo(expected)
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
