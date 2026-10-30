package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GardenSoundsTest {
    @Test
    fun `a note of the preview is a single blip of its pitch on the square, as loud as the lines cleared`() {
        // When
        val note = GardenSounds.note(Pitch.parse("E4"))

        // Then
        assertThat(note).isEqualTo(
            SoundEffect(
                Waveform.SQUARE,
                volume = SoundEffects.linesCleared.volume,
                Envelope(attackMillis = 2, releaseMillis = 15),
                blipsOf("E4" to 100),
            ),
        )
    }

    @Test
    fun `the notes of the preview follow each other a little before the end of the one before`() {
        // When / Then: a rising arpeggio, each note ringing a little under the next
        assertThat(GardenSounds.NOTE_STEP_MILLIS).isEqualTo(80)
        assertThat(GardenSounds.NOTE_STEP_MILLIS).isLessThan(GardenSounds.note(Pitch.parse("C4")).millis)
    }
}
