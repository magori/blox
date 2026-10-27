package ch.magori.blox.feature.musics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The gauge under the row of a music listened to: a bar per bar of the music. */
internal class GaugeFillsTest {
    @Test
    fun `the gauge fills the bars played, the one playing included, and leaves the others empty`() {
        // When
        val fills = gaugeFills(Listening(bar = 3, bars = 8))

        // Then
        assertThat(fills).containsExactly(true, true, true, false, false, false, false, false)
    }
}
