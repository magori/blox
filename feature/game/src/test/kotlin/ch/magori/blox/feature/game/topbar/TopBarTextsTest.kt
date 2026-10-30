package ch.magori.blox.feature.game.topbar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TopBarTextsTest {
    @ParameterizedTest(name = "best {0} -> {1}")
    @CsvSource("0, 000000", "1200, 001200", "1234567, 999999")
    fun `the best is shown as the score in the game, capped at 999 999`(
        best: Int,
        expected: String,
    ) {
        assertThat(topBarTexts(best).best).isEqualTo(expected)
    }

    @ParameterizedTest(name = "best {0} -> TalkBack reads {1}")
    @CsvSource("1200, 1200", "1234567, 1234567")
    fun `TalkBack reads the real best, without padding zeros nor cap`(
        best: Int,
        expected: String,
    ) {
        assertThat(topBarTexts(best).spokenBest).isEqualTo(expected)
    }
}
