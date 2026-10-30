package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CellTest {
    @ParameterizedTest
    @CsvSource("-1, 0", "8, 0", "0, -1", "0, 8")
    fun `a cell lies in one of the eight bars, on one of their eight eighth notes`(
        bar: Int,
        eighth: Int,
    ) {
        assertThatIllegalArgumentException().isThrownBy { Cell(bar, eighth) }
    }

    @ParameterizedTest
    @CsvSource("0, 0, 0", "7, 0, 7", "8, 1, 0", "11, 1, 3", "63, 7, 7")
    fun `the eighth note of the music lies on a cell, the ninth on the first cell of the second bar`(
        eighthOfMusic: Int,
        bar: Int,
        eighth: Int,
    ) {
        assertThat(cellAt(eighthOfMusic)).isEqualTo(Cell(bar, eighth))
    }
}
