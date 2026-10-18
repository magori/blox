package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LineClearTest {
    @ParameterizedTest(name = "{0} lines cleared at once is refused")
    @CsvSource("-1", "5")
    fun `a number of lines cleared at once outside 0 to 4 is refused`(count: Int) {
        assertThatIllegalArgumentException().isThrownBy { LineClear(count) }
    }

    @ParameterizedTest(name = "{0} line(s) at once -> four lines: {1}")
    @CsvSource("4, true", "3, false", "0, false")
    fun `only 4 lines cleared at once make four lines`(
        count: Int,
        expected: Boolean,
    ) {
        assertThat(LineClear(count).isFourLines).isEqualTo(expected)
    }
}
