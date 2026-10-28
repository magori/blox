package ch.magori.blox.feature.garden

import ch.magori.blox.core.designsystem.text.NoteStep
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.music.Accidental
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** What TalkBack and the information row tell of the garden: the notes of the rows, the scale. */
class GardenReadingTest {
    @Test
    fun `each row is named by its note in the scale of the mood, from the bottom, flats in the minor moods`() {
        // When the 7 rows from the bottom of the mysterious mood, C harmonic minor, are named
        val names = (Garden.ROWS - 1 downTo Garden.ROWS - 7).map { noteNameOf(GardenMood.MYSTERIOUS, it) }

        // Then
        assertThat(names).containsExactly(
            NoteName(NoteStep.DO),
            NoteName(NoteStep.RE),
            NoteName(NoteStep.MI, Accidental.FLAT),
            NoteName(NoteStep.FA),
            NoteName(NoteStep.SOL),
            NoteName(NoteStep.LA, Accidental.FLAT),
            NoteName(NoteStep.SI),
        )
    }

    @Test
    fun `the rows of the calm mood are named by the natural notes of its pentatonic scale`() {
        // When the 6 rows from the bottom of C major pentatonic are named
        val names = (Garden.ROWS - 1 downTo Garden.ROWS - 6).map { noteNameOf(GardenMood.CALM, it).step }

        // Then
        assertThat(names).containsExactly(NoteStep.DO, NoteStep.RE, NoteStep.MI, NoteStep.SOL, NoteStep.LA, NoteStep.DO)
    }

    @Test
    fun `each mood is told by the name of its scale`() {
        assertThat(GardenMood.entries.map(::scaleNameOf)).containsExactly(
            R.string.feature_garden_scale_major,
            R.string.feature_garden_scale_pentatonic,
            R.string.feature_garden_scale_harmonic_minor,
            R.string.feature_garden_scale_natural_minor,
        )
    }
}
