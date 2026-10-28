package ch.magori.blox.core.designsystem.text

import ch.magori.blox.core.designsystem.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The words TalkBack reads of notes and chords, shared by the music editor, the Locksmith and the musical garden. */
class MusicTextsTest {
    @Test
    fun `a chord is told with the word of its quality after its root`() {
        assertThat(SpokenChordQuality.entries.map { it.text }).containsExactly(
            R.string.core_designsystem_chord_major,
            R.string.core_designsystem_chord_minor,
            R.string.core_designsystem_chord_diminished,
        )
    }

    @Test
    fun `a note or the root of a chord is told flat or sharp after its name, and as it is when natural`() {
        assertThat(SpokenAccidental.entries.map { it.text }).containsExactly(
            R.string.core_designsystem_flat,
            null,
            R.string.core_designsystem_sharp,
        )
    }

    @Test
    fun `the seven notes are named in French solfège, from C`() {
        assertThat(NoteStep.entries.map { it.text }).containsExactly(
            R.string.core_designsystem_note_do,
            R.string.core_designsystem_note_re,
            R.string.core_designsystem_note_mi,
            R.string.core_designsystem_note_fa,
            R.string.core_designsystem_note_sol,
            R.string.core_designsystem_note_la,
            R.string.core_designsystem_note_si,
        )
    }
}
