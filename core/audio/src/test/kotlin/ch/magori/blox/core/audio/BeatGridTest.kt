package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BeatGridTest {
    @Test
    fun `the next beat of a position inside a beat is the following one`() {
        // Given 4 beats of 3 samples
        val grid = BeatGrid(length = 12, beats = 4)

        // When
        val beat = grid.nextBeat(position = 4)

        // Then
        assertThat(beat).isEqualTo(2)
        assertThat(grid.startOf(beat)).isEqualTo(6)
    }

    @Test
    fun `a position on the start of a beat is that beat`() {
        // Given
        val grid = BeatGrid(length = 12, beats = 4)

        // When / Then
        assertThat(grid.nextBeat(position = 6)).isEqualTo(2)
    }

    @Test
    fun `the start of a beat is rounded to the nearest sample, and a position on it is that beat`() {
        // Given 4 beats of 1.5 samples: they start at 0, 1.5, 3 and 4.5
        val grid = BeatGrid(length = 6, beats = 4)

        // When / Then
        assertThat((0..3).map(grid::startOf)).containsExactly(0, 2, 3, 5)
        assertThat(grid.nextBeat(position = 2)).isEqualTo(1)
        assertThat(grid.nextBeat(position = 5)).isEqualTo(3)
    }

    @Test
    fun `after the last beat comes the end of the loop`() {
        // Given
        val grid = BeatGrid(length = 12, beats = 4)

        // When
        val beat = grid.nextBeat(position = 10)

        // Then: beat 4 starts where the buffer ends, where the loop starts again
        assertThat(beat).isEqualTo(4)
        assertThat(grid.startOf(beat)).isEqualTo(12)
    }

    @Test
    fun `near the end of a buffer of 1 point 3 million samples nothing overflows`() {
        // Given a long buffer of many beats: position x beats is beyond the range of an Int
        val grid = BeatGrid(length = 1_300_000, beats = 2_048)

        // When
        val beat = grid.nextBeat(position = 1_299_999)

        // Then
        assertThat(beat).isEqualTo(2_048)
        assertThat(grid.startOf(beat)).isEqualTo(1_300_000)
        assertThat(grid.startOf(2_047)).isEqualTo(1_299_365)
    }
}
