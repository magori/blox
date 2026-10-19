package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class ScaleTest {
    @ParameterizedTest(name = "{1} on {0}: {2}")
    @CsvSource(
        "C4, MAJOR, C4 D4 E4 F4 G4 A4 B4",
        "F4, MAJOR, F4 G4 A4 A#4 C5 D5 E5",
        "A3, NATURAL_MINOR, A3 B3 C4 D4 E4 F4 G4",
        "A3, HARMONIC_MINOR, A3 B3 C4 D4 E4 F4 G#4",
        "D4, DORIAN, D4 E4 F4 G4 A4 B4 C5",
        "G3, MIXOLYDIAN, G3 A3 B3 C4 D4 E4 F4",
        "C4, MAJOR_PENTATONIC, C4 D4 E4 G4 A4",
    )
    fun `each scale type stacks its own steps above the tonic`(
        tonic: String,
        type: ScaleType,
        expected: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse(tonic), type)

        // When
        val pitches = scale.pitches

        // Then
        assertThat(pitches.joinToString(" ")).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0}: {1} {2}")
    @CsvSource(
        "MAJOR, G4, MAJOR",
        "HARMONIC_MINOR, G4, MAJOR",
        "MAJOR_PENTATONIC, G4, MAJOR",
        "NATURAL_MINOR, G4, MINOR",
        "DORIAN, G4, MINOR",
        "MIXOLYDIAN, G4, MINOR",
    )
    fun `the triad on the fifth degree stacks two thirds of the scale, major V or minor v, major V for a pentatonic`(
        type: ScaleType,
        root: String,
        quality: ChordQuality,
    ) {
        // Given
        val scale = Scale(Pitch.parse("C4"), type)

        // When
        val fifth = scale.fifthTriad()

        // Then
        assertThat(fifth).isEqualTo(Chord(Pitch.parse(root), quality))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ScaleType::class)
    fun `every scale type spans exactly one octave, so the degree after the last one is the tonic an octave up`(
        type: ScaleType,
    ) {
        // Given
        val scale = Scale(Pitch.parse("C4"), type)

        // When
        val nextOctave = scale.degree(scale.pitches.size + 1)

        // Then
        assertThat(nextOctave).isEqualTo(Pitch.parse("C5"))
    }

    @ParameterizedTest(name = "degree {1} of {0} is {2}")
    @CsvSource(
        "MAJOR, 8, C5",
        "MAJOR, 10, E5",
        "HARMONIC_MINOR, 15, C6",
        "MAJOR_PENTATONIC, 6, C5",
        "MAJOR_PENTATONIC, 8, E5",
    )
    fun `degrees past the seventh go on in the octaves above`(
        type: ScaleType,
        degree: Int,
        expected: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse("C4"), type)

        // When
        val pitch = scale.degree(degree)

        // Then
        assertThat(pitch).isEqualTo(Pitch.parse(expected))
    }

    @ParameterizedTest(name = "degree {1} of {0} is {2}")
    @CsvSource(
        "MAJOR, 0, B3",
        "MAJOR, -6, C3",
        "HARMONIC_MINOR, 0, B3",
        "HARMONIC_MINOR, -1, G#3",
        "MAJOR_PENTATONIC, 0, A3",
        "MAJOR_PENTATONIC, -4, C3",
    )
    fun `degrees below the first go down under the tonic, degree 0 being the one just below`(
        type: ScaleType,
        degree: Int,
        expected: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse("C4"), type)

        // When
        val pitch = scale.degree(degree)

        // Then
        assertThat(pitch).isEqualTo(Pitch.parse(expected))
    }

    @ParameterizedTest(name = "{0} on A4 holds {1}: {2}")
    @CsvSource(
        "HARMONIC_MINOR, A4, true",
        "HARMONIC_MINOR, G#2, true",
        "HARMONIC_MINOR, G#7, true",
        "HARMONIC_MINOR, G5, false",
        "NATURAL_MINOR, G1, true",
        "NATURAL_MINOR, G#4, false",
        "MAJOR_PENTATONIC, F#3, true",
        "MAJOR_PENTATONIC, D6, false",
    )
    fun `a scale holds the pitches of its degrees in every octave and no other`(
        type: ScaleType,
        pitch: String,
        expected: Boolean,
    ) {
        // Given
        val scale = Scale(Pitch.parse("A4"), type)

        // When / Then
        assertThat(scale.contains(Pitch.parse(pitch))).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{2} in {1} on {0}: {3}")
    @CsvSource(
        "C4, MAJOR, I, C4 E4 G4",
        "C4, MAJOR, ii, D4 F4 A4",
        "C4, MAJOR, V, G4 B4 D5",
        "C4, MAJOR, vi, A4 C5 E5",
        "C4, MAJOR, vii°, B4 D5 F5",
        "C4, NATURAL_MINOR, i, C4 D#4 G4",
        "C4, NATURAL_MINOR, VI, G#4 C5 D#5",
        "C4, NATURAL_MINOR, VII, A#4 D5 F5",
        "A3, HARMONIC_MINOR, V, E4 G#4 B4",
        "D4, DORIAN, IV, G4 B4 D5",
        "G3, MIXOLYDIAN, VII, F4 A4 C5",
    )
    fun `a numeral builds its chord on the matching degree of the scale`(
        tonic: String,
        type: ScaleType,
        numeral: String,
        expected: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse(tonic), type)

        // When
        val chord = scale.chord(RomanNumeral.parse(numeral))

        // Then
        assertThat(chord.pitches.joinToString(" ")).isEqualTo(expected)
    }

    @Test
    fun `the case of the numeral sets the chord, even with a note outside the scale like the major V in minor`() {
        // Given
        val scale = Scale(Pitch.parse("A3"), ScaleType.NATURAL_MINOR)

        // When
        val chord = scale.chord(RomanNumeral.parse("V"))

        // Then
        assertThat(chord.pitches.joinToString(" ")).isEqualTo("E4 G#4 B4")
        assertThat(Pitch.parse("G#4") in scale).isFalse()
    }

    @ParameterizedTest(name = "♭VII in {0} on {1} is {2} major")
    @CsvSource(
        "HARMONIC_MINOR, A3, G4",
        "MAJOR, C4, A#4",
    )
    fun `a flat numeral builds its chord a semitone under the degree of the scale`(
        type: ScaleType,
        tonic: String,
        root: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse(tonic), type)

        // When
        val chord = scale.chord(RomanNumeral.parse("♭VII"))

        // Then
        assertThat(chord).isEqualTo(Chord(Pitch.parse(root), ChordQuality.MAJOR))
    }

    @Test
    fun `a sharp numeral builds its chord a semitone above the degree of the scale`() {
        // Given
        val scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR)

        // When
        val chord = scale.chord(RomanNumeral.parse("#iv°"))

        // Then
        assertThat(chord).isEqualTo(Chord(Pitch.parse("F#4"), ChordQuality.DIMINISHED))
    }

    @Test
    fun `a pentatonic scale takes its chords from the major scale on its tonic, but keeps its own five notes`() {
        // Given
        val scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR_PENTATONIC)

        // When
        val chord = scale.chord(RomanNumeral.parse("IV"))

        // Then
        assertThat(chord).isEqualTo(Chord(Pitch.parse("F4"), ChordQuality.MAJOR))
        assertThat(Pitch.parse("F4") in scale).isFalse()
    }

    @Test
    fun `a scale on the highest pitch still knows its degrees, though they go past B8`() {
        // Given
        val scale = Scale(Pitch.parse("B8"), ScaleType.MAJOR)

        // When / Then
        assertThat(Pitch.parse("C#2") in scale).isTrue()
        assertThat(Pitch.parse("C2") in scale).isFalse()
    }

    @Test
    fun `a degree past B8 or under C0 is refused, and so are the pitches of a scale that goes past B8`() {
        // Given
        val high = Scale(Pitch.parse("B8"), ScaleType.MAJOR)
        val low = Scale(Pitch.parse("C0"), ScaleType.MAJOR)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { high.degree(2) }
        assertThatIllegalArgumentException().isThrownBy { high.pitches }
        assertThatIllegalArgumentException().isThrownBy { low.degree(0) }
    }
}
