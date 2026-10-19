package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class ProgressionTest {
    private val cMajor = Scale(Pitch.parse("C4"), ScaleType.MAJOR)
    private val cMajorOneFiveSixFour = listOf(
        Chord(Pitch.parse("C4"), ChordQuality.MAJOR),
        Chord(Pitch.parse("G4"), ChordQuality.MAJOR),
        Chord(Pitch.parse("A4"), ChordQuality.MINOR),
        Chord(Pitch.parse("F4"), ChordQuality.MAJOR),
    )

    @Test
    fun `each bar holds the next chord of the progression`() {
        // Given
        val progression = Progression.parse("I-V-vi-IV")

        // When
        val chords = (0..3).map { bar -> progression.chordAt(bar, cMajor) }

        // Then
        assertThat(chords).containsExactlyElementsOf(cMajorOneFiveSixFour)
    }

    @Test
    fun `after its last chord the progression starts again, so four chords fill eight bars twice over`() {
        // Given
        val progression = Progression.parse("I-V-vi-IV")

        // When
        val secondHalf = (4..7).map { bar -> progression.chordAt(bar, cMajor) }

        // Then
        assertThat(secondHalf).containsExactlyElementsOf(cMajorOneFiveSixFour)
    }

    @Test
    fun `a progression over a pentatonic scale plays the chords of the major scale on its tonic`() {
        // Given
        val progression = Progression.parse("I-vi-IV-V")

        // When
        val chord = progression.chordAt(2, Scale(Pitch.parse("C4"), ScaleType.MAJOR_PENTATONIC))

        // Then
        assertThat(chord).isEqualTo(Chord(Pitch.parse("F4"), ChordQuality.MAJOR))
    }

    @Test
    fun `numerals may be separated by en dashes, as the specification writes them`() {
        // When
        val progression = Progression.parse("i–♭VII–VI–V")

        // Then
        assertThat(progression).isEqualTo(Progression.parse("i-♭VII-VI-V"))
    }

    @Test
    fun `a progression without chords is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Progression(emptyList()) }
    }

    @Test
    fun `bars are counted from 0, so a negative bar is refused`() {
        // Given
        val progression = Progression.parse("I-V-vi-IV")

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { progression.chordAt(-1, cMajor) }
    }
}
