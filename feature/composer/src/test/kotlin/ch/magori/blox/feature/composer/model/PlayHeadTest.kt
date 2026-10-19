package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlayHeadTest {
    @Test
    fun `the play head starts on the first cell of the first bar`() {
        assertThat(PlayHead.START.cell).isEqualTo(Cell(bar = 0, eighth = 0))
    }

    @Test
    fun `the play head moves on a cell each eighth note, 15 ticks at 120 beats per minute`() {
        // Given
        val tempo = Tempo(120)

        // When
        val early = PlayHead.START.advance(ticks = 14, tempo)
        val second = early.advance(ticks = 1, tempo)
        val tenth = second.advance(ticks = 8 * 15, tempo)

        // Then
        assertThat(early.cell).isEqualTo(Cell(bar = 0, eighth = 0))
        assertThat(second.cell).isEqualTo(Cell(bar = 0, eighth = 1))
        assertThat(tenth.cell).isEqualTo(Cell(bar = 1, eighth = 1))
    }

    @Test
    fun `the play head comes back to the first cell after the eighth bar, as the music loops`() {
        // Given the last cell of the eighth bar, at 60 beats per minute: 30 ticks a cell
        val last = PlayHead.START.advance(ticks = 63 * 30, Tempo(60))

        // When
        val looped = last.advance(ticks = 30, Tempo(60))

        // Then
        assertThat(last.cell).isEqualTo(Cell(bar = 7, eighth = 7))
        assertThat(looped.cell).isEqualTo(Cell(bar = 0, eighth = 0))
    }

    @Test
    fun `after a change of tempo, the play head moves on at the new tempo from where it stood`() {
        // Given half a cell at 60 beats per minute, 15 ticks of 30
        val half = PlayHead.START.advance(ticks = 15, Tempo(60))

        // When the other half at 120 beats per minute, 7 ticks of 15, then one more tick
        val almost = half.advance(ticks = 7, Tempo(120))
        val next = almost.advance(ticks = 1, Tempo(120))

        // Then
        assertThat(almost.cell).isEqualTo(Cell(bar = 0, eighth = 0))
        assertThat(next.cell).isEqualTo(Cell(bar = 0, eighth = 1))
    }
}
