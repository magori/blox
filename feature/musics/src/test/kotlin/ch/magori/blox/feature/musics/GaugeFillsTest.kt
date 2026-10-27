package ch.magori.blox.feature.musics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** The gauge under the row of a music listened to: a bar per bar of the music. */
internal class GaugeFillsTest {
    @ParameterizedTest(name = "bar {0} of 8: {1}")
    @CsvSource(
        "1, X.......",
        "3, XXX.....",
        "8, XXXXXXXX",
    )
    fun `the gauge fills the bars played, the one playing included, and leaves the others empty`(
        bar: Int,
        expected: String,
    ) {
        // When
        val fills = gaugeFills(Listening(bar = bar, bars = 8))

        // Then
        assertThat(fills.joinToString("") { if (it) "X" else "." }).isEqualTo(expected)
    }
}
