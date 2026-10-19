package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class MelodyMeasuresTest {
    @Test
    fun `the share of steps counts the moves to a neighbouring note of the scale, repeats and rests left out`() {
        // Given C D D, a rest, then E G A: of the moves C-D, D-E, E-G and G-A, three are steps
        val melody = melodyOf("C4 1, D4 1, D4 1, r 1", "E4 1, G4 1, A4 2")

        // When
        val share = melody.stepShare(C_MAJOR)

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `in a pentatonic scale, a minor third between two neighbouring degrees is a step`() {
        // Given E and G, neighbours in the C major pentatonic scale, then G and C, a fourth
        val melody = melodyOf("E4 1, G4 1, C5 2")

        // When
        val share = melody.stepShare(Scale(Pitch.parse("C4"), ScaleType.MAJOR_PENTATONIC))

        // Then
        assertThat(share).isCloseTo(0.5, within(1e-9))
    }

    @Test
    fun `in a seven-note scale, the augmented second of the harmonic minor is a leap, not a step`() {
        // Given A flat, written G sharp, then B, neighbours in C harmonic minor but three semitones apart
        val melody = melodyOf("G#4 2, B4 2")

        // When
        val share = melody.stepShare(Scale(Pitch.parse("C4"), ScaleType.HARMONIC_MINOR))

        // Then
        assertThat(share).isZero()
    }

    @Test
    fun `the peak of a melody lists the bars where its highest note is struck, once for each time`() {
        // Given G4 the highest note, struck twice in bar 2 and once in bar 3, counted from 0
        val melody = melodyOf("C4 2, E4 2", "G4 1, F4 1, G4 2", "r 2, G4 2")

        // When
        val peak = melody.peakBars

        // Then
        assertThat(peak).containsExactly(1, 1, 2)
    }

    @Test
    fun `the share of fifths counts the moves of a perfect fifth, up or down, repeats left out`() {
        // Given C G, a fifth up, G G, a repeat, G C, a fifth down, C D, a step, and D A, a fifth up across a rest
        val melody = melodyOf("C4 1, G4 1, G4 1, C4 1", "D4 1, r 1, A4 2")

        // When
        val share = melody.fifthShare

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `the share of descents counts the moves down, repeats left out`() {
        // Given G F E, two moves down, E E, a repeat, then E C up an octave and C5 B4 down
        val melody = melodyOf("G4 1, F4 1, E4 1, E4 1", "C5 2, B4 2")

        // When
        val share = melody.descentShare

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `the share of arpeggios counts the moves between two notes of the chord in place, repeats left out`() {
        // Given C E G in C major, two moves within the chord, then G F against F major, a step out of the chord to it,
        // and F A within it
        val melody = melodyOf("C4 1, E4 1, G4 2", "G4 1, F4 1, A4 2")
        val bars = listOf(barOn("C4"), barOn("F4"))

        // When
        val share = melody.arpeggioShare(bars)

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    /** A bar playing the major chord on [root] all along. */
    private fun barOn(root: String): Bar =
        Bar(Section.A, BarRole.MOTIF, listOf(Chord(Pitch.parse(root), ChordQuality.MAJOR)), RhythmCell.parse("4"))

    private companion object {
        val C_MAJOR = Scale(Pitch.parse("C4"), ScaleType.MAJOR)
    }
}

/** A melody written bar by bar, as notes such as `"C4 1"` separated by commas, `r` marking a rest. */
private fun melodyOf(vararg bars: String): Melody = Melody(
    bars.map { bar ->
        bar.split(", ").map { note ->
            val (pitch, beats) = note.split(" ")
            Note(if (pitch == "r") null else Pitch.parse(pitch), beats.toDouble())
        }
    },
)
