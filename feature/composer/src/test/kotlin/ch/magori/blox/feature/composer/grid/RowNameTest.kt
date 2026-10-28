package ch.magori.blox.feature.composer.grid

import ch.magori.blox.core.designsystem.text.NoteStep
import ch.magori.blox.core.music.MELODY_RANGE
import ch.magori.blox.feature.composer.model.pitch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RowNameTest {
    @Test
    fun `a line is named after its note without octave, low in the first octave of the track and high in the second`() {
        // When
        val low = rowName(pitch("G4"), MELODY_RANGE)
        val high = rowName(pitch("G5"), MELODY_RANGE)

        // Then
        assertThat(low).isEqualTo(RowName(NoteStep.SOL, sharp = false, Register.LOW))
        assertThat(high).isEqualTo(RowName(NoteStep.SOL, sharp = false, Register.HIGH))
    }

    @Test
    fun `the twelve notes of an octave are named with sharps, as the pitches are written`() {
        // Given
        val octave = listOf("C5", "C#5", "D5", "D#5", "E5", "F5", "F#5", "G5", "G#5", "A5", "A#5", "B5")

        // When
        val names = octave.map { rowName(pitch(it), MELODY_RANGE).let { name -> name.step to name.sharp } }

        // Then
        assertThat(names).containsExactly(
            NoteStep.DO to false,
            NoteStep.DO to true,
            NoteStep.RE to false,
            NoteStep.RE to true,
            NoteStep.MI to false,
            NoteStep.FA to false,
            NoteStep.FA to true,
            NoteStep.SOL to false,
            NoteStep.SOL to true,
            NoteStep.LA to false,
            NoteStep.LA to true,
            NoteStep.SI to false,
        )
    }

    @Test
    fun `the top of the range, a third octave, is very high, and a note opened below the range very low`() {
        // When
        val top = rowName(pitch("C6"), MELODY_RANGE)
        val below = rowName(pitch("B3"), MELODY_RANGE)

        // Then
        assertThat(top).isEqualTo(RowName(NoteStep.DO, sharp = false, Register.VERY_HIGH))
        assertThat(below).isEqualTo(RowName(NoteStep.SI, sharp = false, Register.VERY_LOW))
    }
}
