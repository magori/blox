package ch.magori.blox.feature.composer.model

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
}
