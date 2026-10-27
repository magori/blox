package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class LocksmithSoundsTest {
    private val aMinor = Chord(Pitch.parse("A4"), ChordQuality.MINOR)

    @Test
    fun `an arpeggio plays the root the third and the fifth upwards, the fifth held, as loud as the lines cleared`() {
        // When
        val arpeggio = LocksmithSounds.arpeggio(aMinor)

        // Then: two blips of 70 ms, then the fifth held 300 ms
        assertThat(arpeggio).isEqualTo(
            SoundEffect(
                Waveform.SQUARE,
                volume = SoundEffects.linesCleared.volume,
                Envelope(attackMillis = 2, releaseMillis = 15),
                blipsOf("A4" to 70, "C5" to 70, "E5" to 300),
            ),
        )
    }

    @Test
    fun `the bass plays the root two octaves down for the whole arpeggio, on the triangle, as loud as the game over`() {
        // When
        val bass = LocksmithSounds.bass(aMinor)

        // Then
        assertThat(bass).isEqualTo(
            SoundEffect(
                Waveform.TRIANGLE,
                volume = SoundEffects.gameOver.volume,
                Envelope(attackMillis = 2, releaseMillis = 30),
                blipsOf("A2" to LocksmithSounds.arpeggio(aMinor).millis),
            ),
        )
    }

    @Test
    fun `the arpeggio and the bass of a chord are two distinct effects, so that they play on two tracks`() {
        // When
        val effects = LocksmithSounds.effectsOf(aMinor)

        // Then
        assertThat(effects)
            .containsExactly(LocksmithSounds.arpeggio(aMinor), LocksmithSounds.bass(aMinor))
            .doesNotHaveDuplicates()
    }

    @ParameterizedTest(name = "a root {0} is refused")
    @ValueSource(strings = ["B3", "C#6"])
    fun `a chord whose bass would fall out of the range of the basses, C2 to C4, is refused`(root: String) {
        // Given: two octaves below B3 is B1, under C2; below C#6, C#4, above C4
        val chord = Chord(Pitch.parse(root), ChordQuality.MAJOR)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { LocksmithSounds.bass(chord) }
    }

    @ParameterizedTest(name = "a root {0} has a bass {1}")
    @CsvSource("C4, C2", "C6, C4")
    fun `the bass of a chord on C4 to C6 is in the range of the basses, its ends included`(
        root: String,
        bass: String,
    ) {
        // Given
        val chord = Chord(Pitch.parse(root), ChordQuality.MAJOR)

        // When / Then
        assertThat(LocksmithSounds.bass(chord).blips.single().pitch).isEqualTo(Pitch.parse(bass))
    }

    @Test
    fun `the opening flourish climbs the tonic chord, C5 to C6, in 420 ms, as loud as four lines cleared`() {
        // Given / When / Then
        assertThat(LocksmithSounds.openingFlourish).isEqualTo(
            SoundEffect(
                Waveform.SQUARE,
                volume = SoundEffects.fourLinesCleared.volume,
                Envelope(attackMillis = 2, releaseMillis = 15),
                blipsOf("C5" to 60, "E5" to 60, "G5" to 60, "C6" to 240),
            ),
        )
    }

    @Test
    fun `the refusal falls a semitone, C3 then B2, in 180 ms on the square`() {
        // Given / When / Then
        assertThat(LocksmithSounds.refusal).isEqualTo(
            SoundEffect(
                Waveform.SQUARE,
                volume = 0.06,
                Envelope(attackMillis = 2, releaseMillis = 15),
                blipsOf("C3" to 90, "B2" to 90),
            ),
        )
    }

    @Test
    fun `every triad on C4 to B4 has an arpeggio and a bass in the range of the basses`() {
        // Given
        val chords = (0 until 12).flatMap { semitones ->
            ChordQuality.entries.map { quality -> Chord(Pitch(Pitch.parse("C4").midi + semitones), quality) }
        }

        // When / Then
        assertThat(chords).allSatisfy { chord ->
            assertThat(LocksmithSounds.arpeggio(chord).blips).hasSize(3)
            assertThat(LocksmithSounds.bass(chord).blips.single().pitch).matches { it in BASS_RANGE }
        }
    }

    @Test
    fun `every locksmith effect stays within the volume of the effects on its waveform`() {
        // Given
        val effects = LocksmithSounds.effectsOf(aMinor) + LocksmithSounds.openingFlourish + LocksmithSounds.refusal

        // When / Then
        assertThat(effects).allSatisfy { effect ->
            val volumes = SoundEffects.all.filter { it.waveform == effect.waveform }.map { it.volume }
            assertThat(effect.volume).isBetween(volumes.min(), volumes.max())
        }
    }
}
