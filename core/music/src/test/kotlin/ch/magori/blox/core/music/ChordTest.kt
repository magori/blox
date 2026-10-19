package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ChordTest {
    @ParameterizedTest(name = "{1} on {0}: {2}")
    @CsvSource(
        "C4, MAJOR, C4 E4 G4",
        "A3, MINOR, A3 C4 E4",
        "B3, DIMINISHED, B3 D4 F4",
    )
    fun `a triad stacks two thirds on its root, major then minor, minor then major, or two minor`(
        root: String,
        quality: ChordQuality,
        expected: String,
    ) {
        // Given
        val chord = Chord(Pitch.parse(root), quality)

        // When
        val pitches = chord.pitches

        // Then
        assertThat(pitches.joinToString(" ")).isEqualTo(expected)
    }

    @ParameterizedTest(name = "A minor holds {0}: {1}")
    @CsvSource(
        "A3, true",
        "C1, true",
        "E7, true",
        "B3, false",
        "C#4, false",
        "D#4, false",
    )
    fun `a chord holds the pitches of its root, third and fifth in every octave and no other`(
        pitch: String,
        expected: Boolean,
    ) {
        // Given
        val chord = Chord(Pitch.parse("A3"), ChordQuality.MINOR)

        // When / Then
        assertThat(chord.contains(Pitch.parse(pitch))).isEqualTo(expected)
    }

    @Test
    fun `a chord on the highest pitch still knows its notes, though its fifth goes past B8`() {
        // Given
        val chord = Chord(Pitch.parse("B8"), ChordQuality.MAJOR)

        // When / Then
        assertThat(Pitch.parse("F#3") in chord).isTrue()
        assertThat(Pitch.parse("F3") in chord).isFalse()
    }

    @Test
    fun `the pitches of a chord whose fifth goes past B8 are refused`() {
        // Given
        val chord = Chord(Pitch.parse("B8"), ChordQuality.MAJOR)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { chord.pitches }
    }
}
