package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VoiceTest {
    @ParameterizedTest(name = "a volume of {0} is refused")
    @ValueSource(doubles = [-0.01, 1.01])
    fun `the volume must be between 0 and 1`(volume: Double) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy {
            aVoice(volume)
        }
    }

    @ParameterizedTest(name = "a volume of {0} is accepted")
    @ValueSource(doubles = [0.0, 1.0])
    fun `a volume of 0 or 1 is accepted`(volume: Double) {
        // Given / When / Then
        assertThatNoException().isThrownBy {
            aVoice(volume)
        }
    }

    @Test
    fun `the total duration is the sum of the durations of the notes`() {
        // Given a bar of four beats, a rest included
        val voice = Voice(
            waveform = Waveform.SQUARE,
            volume = 0.07,
            notes = listOf(
                Note(Pitch.parse("E5"), 1.0),
                Note(Pitch.parse("B4"), 0.5),
                Note(Pitch.parse("C5"), 0.5),
                Note(Pitch.parse("D5"), 1.0),
                Note(pitch = null, beats = 0.5),
                Note(Pitch.parse("B4"), 0.5),
            ),
        )

        // When / Then
        assertThat(voice.beats).isEqualTo(4.0)
    }

    @Test
    fun `a voice needs at least one note`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Voice(Waveform.SQUARE, volume = 0.07, notes = emptyList()) }
    }
}
