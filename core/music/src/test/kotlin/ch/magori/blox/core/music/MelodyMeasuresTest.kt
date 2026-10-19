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
        val share = MelodyMeasures(formIn(C_MAJOR, "C", "C"), melody).stepShare

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `in a pentatonic scale, a minor third between two neighbouring degrees is a step`() {
        // Given E and G, neighbours in the C major pentatonic scale, then G and C, a fourth
        val melody = melodyOf("E4 1, G4 1, C5 2")
        val form = formIn(Scale(Pitch.parse("C4"), ScaleType.MAJOR_PENTATONIC), "C")

        // When
        val share = MelodyMeasures(form, melody).stepShare

        // Then
        assertThat(share).isCloseTo(0.5, within(1e-9))
    }

    @Test
    fun `in a seven-note scale, the augmented second of the harmonic minor is a leap, not a step`() {
        // Given A flat, written G sharp, then B, neighbours in C harmonic minor but three semitones apart
        val melody = melodyOf("G#4 2, B4 2")
        val form = formIn(Scale(Pitch.parse("C4"), ScaleType.HARMONIC_MINOR), "G")

        // When
        val share = MelodyMeasures(form, melody).stepShare

        // Then
        assertThat(share).isZero()
    }

    @Test
    fun `the peak of a melody lists the bars where its highest note is struck, once for each time`() {
        // Given G4 the highest note, struck twice in bar 2 and once in bar 3, counted from 0
        val melody = melodyOf("C4 2, E4 2", "G4 1, F4 1, G4 2", "r 2, G4 2")

        // When
        val peak = MelodyMeasures(formIn(C_MAJOR, "C", "C", "C"), melody).peakBars

        // Then
        assertThat(peak).containsExactly(1, 1, 2)
    }

    @Test
    fun `the share of fifths counts the moves of a perfect fifth, up or down, repeats left out`() {
        // Given C G, a fifth up, G G, a repeat, G C, a fifth down, C D, a step, and D A, a fifth up across a rest
        val melody = melodyOf("C4 1, G4 1, G4 1, C4 1", "D4 1, r 1, A4 2")

        // When
        val share = MelodyMeasures(formIn(C_MAJOR, "C", "C"), melody).fifthShare

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `the share of descents counts the moves down, repeats left out`() {
        // Given G F E, two moves down, E E, a repeat, then E C up an octave and C5 B4 down
        val melody = melodyOf("G4 1, F4 1, E4 1, E4 1", "C5 2, B4 2")

        // When
        val share = MelodyMeasures(formIn(C_MAJOR, "C", "C"), melody).descentShare

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `the share of arpeggios counts the moves between two notes of the chord in place, repeats left out`() {
        // Given C E G in C major, two moves within the chord, then G F against F major, a step out of the chord to it,
        // and F A within it
        val melody = melodyOf("C4 1, E4 1, G4 2", "G4 1, F4 1, A4 2")

        // When
        val share = MelodyMeasures(formIn(C_MAJOR, "C", "F"), melody).arpeggioShare

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `a melody that never moves makes no move of any kind, rather than a share that is not a number`() {
        // Given C struck again and again, a repeat being no move
        val measures = MelodyMeasures(formIn(C_MAJOR, "C"), melodyOf("C4 1, C4 1, C4 2"))

        // When
        val shares = listOf(measures.stepShare, measures.fifthShare, measures.descentShare, measures.arpeggioShare)

        // Then
        assertThat(shares).containsOnly(0.0)
    }

    @Test
    fun `a melody that strikes no note has no peak, rather than failing`() {
        // Given a bar of rest
        val melody = melodyOf("r 4")

        // When
        val peak = MelodyMeasures(formIn(C_MAJOR, "C"), melody).peakBars

        // Then
        assertThat(peak).isEmpty()
    }

    @Test
    fun `the span of a melody counts the semitones from its lowest note to its highest, none without a note`() {
        // Given a melody from B3 up to D5, an octave and a minor third, and a bar of rest
        val melody = melodyOf("C4 1, B3 1, D5 2")

        // When
        val spans = listOf(melody, melodyOf("r 4")).map { MelodyMeasures(formIn(C_MAJOR, "C"), it).span }

        // Then
        assertThat(spans).containsExactly(15, 0)
    }

    @Test
    fun `the unprepared notes are the notes out of the chord in place not both reached and left by step`() {
        // Given, out of the chord: D passing from C to E, A leaped to from E, and G a neighbour of F under F major
        val melody = melodyOf("C4 1, D4 1, E4 1, A4 1", "F4 1, G4 1, F4 2")

        // When
        val unprepared = MelodyMeasures(formIn(C_MAJOR, "C", "F"), melody).unpreparedNotes

        // Then
        assertThat(unprepared).isEqualTo(1)
    }

    @Test
    fun `a note out of the chord that opens the music is reached from the last note, as the music loops`() {
        // Given D, out of the C major chord, reached from the last C as the music loops, and left by step to C
        val melody = melodyOf("D4 2, C4 2")

        // When
        val unprepared = MelodyMeasures(formIn(C_MAJOR, "C"), melody).unpreparedNotes

        // Then
        assertThat(unprepared).isZero()
    }

    @Test
    fun `a melody striking only notes of the chord leaves none unprepared`() {
        // Given C E G under C major
        val melody = melodyOf("C4 1, E4 1, G4 2")

        // When
        val unprepared = MelodyMeasures(formIn(C_MAJOR, "C"), melody).unpreparedNotes

        // Then
        assertThat(unprepared).isZero()
    }

    @Test
    fun `the share of the motif kept counts the moves of the bars taking it up that follow bar 1, in degrees`() {
        // Given a motif going up two steps, taken up a fourth higher, then going down a step and up a step, then a
        // contrast leaping away from it, which does not take it up
        val melody = melodyOf("C4 1, D4 1, E4 2", "F4 1, G4 1, A4 2", "E4 1, D4 1, E4 2", "C5 1, G4 1, C4 2")
        val form = formIn(C_MAJOR, "C", "F", "C", "C").let { form ->
            form.copy(bars = form.bars.mapIndexed { bar, it -> if (bar == 3) it.copy(role = BarRole.CONTRAST) else it })
        }

        // When
        val share = MelodyMeasures(form, melody).motifShare

        // Then
        assertThat(share).isCloseTo(0.75, within(1e-9))
    }

    @Test
    fun `a motif of a single note keeps its contour whole, as it has no move to lose`() {
        // Given a motif of one whole note, taken up in bar 2
        val melody = melodyOf("C4 4", "F4 4")

        // When
        val share = MelodyMeasures(formIn(C_MAJOR, "C", "F"), melody).motifShare

        // Then
        assertThat(share).isEqualTo(1.0)
    }

    @Test
    fun `a note out of the chord struck again is prepared when reached and left by step, the repeat in between`() {
        // Given D, out of the C major chord, struck twice between C and E
        val melody = melodyOf("C4 1, D4 1, D4 1, E4 1")

        // When
        val unprepared = MelodyMeasures(formIn(C_MAJOR, "C"), melody).unpreparedNotes

        // Then
        assertThat(unprepared).isZero()
    }
}
