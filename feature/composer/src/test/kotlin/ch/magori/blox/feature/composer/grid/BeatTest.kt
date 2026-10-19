package ch.magori.blox.feature.composer.grid

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BeatTest {
    @Test
    fun `the eight cells of a bar are the four beats and their halves, two cells a beat`() {
        // When
        val beats = (0 until 8).map(::beatOf)

        // Then
        assertThat(beats).containsExactly(
            Beat(1, half = false),
            Beat(1, half = true),
            Beat(2, half = false),
            Beat(2, half = true),
            Beat(3, half = false),
            Beat(3, half = true),
            Beat(4, half = false),
            Beat(4, half = true),
        )
    }
}
