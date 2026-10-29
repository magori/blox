package ch.magori.blox.feature.locksmith

import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** How many columns the grid of the screen « Serrures » has: tiles of about 80 dp, wider in a large font. */
class PadlockColumnsTest {
    @ParameterizedTest(name = "{0} dp in a font of {1}: {2} columns")
    @CsvSource(
        // A phone of 390 dp, its margins taken off.
        "358, 1.0, 4",
        "358, 1.3, 3",
        "358, 2.0, 2",
        // A phone of 320 dp.
        "288, 1.0, 3",
        "288, 2.0, 2",
        // Narrower than a tile: one column all the same.
        "40, 2.0, 1",
    )
    fun `the larger the font, the fewer the columns, never none`(
        width: Int,
        fontScale: Float,
        columns: Int,
    ) {
        assertThat(padlockColumns(width.dp, fontScale)).isEqualTo(columns)
    }
}
