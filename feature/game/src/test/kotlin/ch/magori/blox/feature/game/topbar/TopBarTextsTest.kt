package ch.magori.blox.feature.game.topbar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TopBarTextsTest {
    @ParameterizedTest(name = "record {0} -> {1}")
    @CsvSource("0, 000000", "1200, 001200", "1234567, 999999")
    fun `the record is shown as the score in the game, capped at 999 999`(
        record: Int,
        expected: String,
    ) {
        assertThat(topBarTexts(record).record).isEqualTo(expected)
    }

    @ParameterizedTest(name = "record {0} -> TalkBack reads {1}")
    @CsvSource("1200, 1200", "1234567, 1234567")
    fun `TalkBack reads the real record, without padding zeros nor cap`(
        record: Int,
        expected: String,
    ) {
        assertThat(topBarTexts(record).spokenRecord).isEqualTo(expected)
    }
}
