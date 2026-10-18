package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class PitchTest {
    @Test
    fun `A4 is 440 Hz`() {
        // Given
        val pitch = Pitch.parse("A4")

        // When
        val frequency = pitch.frequency

        // Then
        assertThat(frequency).isCloseTo(440.0, within(1e-9))
    }

    @ParameterizedTest(name = "{0} is MIDI note {1}")
    @CsvSource("A4, 69", "C4, 60", "E2, 40", "F#5, 78")
    fun `the MIDI number is 12 times the octave plus one, plus the semitone`(
        name: String,
        midi: Int,
    ) {
        // Given
        val pitch = Pitch.parse(name)

        // When / Then
        assertThat(pitch.midi).isEqualTo(midi)
    }

    @ParameterizedTest(name = "{0} is {1} Hz")
    @CsvSource("A5, 880.0", "A3, 220.0")
    fun `one octave higher doubles the frequency and one octave lower halves it`(
        name: String,
        frequency: Double,
    ) {
        // Given
        val pitch = Pitch.parse(name)

        // When / Then
        assertThat(pitch.frequency).isCloseTo(frequency, within(1e-9))
    }

    @Test
    fun `middle C is about 261 point 63 Hz`() {
        // Given
        val pitch = Pitch.parse("C4")

        // When / Then
        assertThat(pitch.frequency).isCloseTo(261.63, within(0.01))
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @ValueSource(strings = ["H4", "A", "", "Ab4", "a4", "A10", "A#", " A4", "A9"])
    fun `an invalid name is refused`(name: String) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Pitch.parse(name) }
    }

    @Test
    fun `two pitches with the same name are equal, and so are two identical notes`() {
        // When
        val first = Pitch.parse("F#5")
        val second = Pitch.parse("F#5")

        // Then
        assertThat(first).isEqualTo(second)
        assertThat(Note(first, beats = 0.5)).isEqualTo(Note(second, beats = 0.5))
    }

    @ParameterizedTest(name = "{0} prints as {0}")
    @ValueSource(strings = ["A4", "F#5", "C0", "C#4", "E2", "B8"])
    fun `a pitch prints as its name`(name: String) {
        // Given
        val pitch = Pitch.parse(name)

        // When / Then
        assertThat(pitch.toString()).isEqualTo(name)
    }

    @ParameterizedTest(name = "MIDI number {0} is refused")
    @ValueSource(ints = [11, 120])
    fun `a MIDI number outside C0 to B8 is refused`(midi: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Pitch(midi) }
    }

    @ParameterizedTest(name = "MIDI number {0} is accepted")
    @ValueSource(ints = [12, 119])
    fun `the MIDI numbers of C0 and B8 are accepted`(midi: Int) {
        // Given / When / Then
        assertThatNoException().isThrownBy { Pitch(midi) }
    }

    @ParameterizedTest(name = "MIDI number {0}")
    @ValueSource(ints = [12, 119])
    fun `a pitch at either end of the range reads back from its name`(midi: Int) {
        // Given
        val pitch = Pitch(midi)

        // When / Then
        assertThat(Pitch.parse(pitch.toString())).isEqualTo(pitch)
    }
}
