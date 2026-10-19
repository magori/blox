package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Scores
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ScoreDraftOpeningTest {
    private val cMajor = Scale(Pitch.parse("C4"), ScaleType.MAJOR)

    @Test
    fun `a score opens with its long notes, its rests and its two voices, on its scale, category and tempo`() {
        // Given
        val score = score(
            melody = thenSilence(notes("E4:1 r:0.5 G4:2.5"), bars = 7),
            bass = thenSilence(notes("C3:4"), bars = 7),
            bpm = 132,
        )

        // When
        val draft = ScoreDraft.fromScore(score, cMajor, Category.JOYFUL)

        // Then
        assertThat(draft).isNotNull
        draft!!
        assertThat(draft.scale).isEqualTo(cMajor)
        assertThat(draft.category).isEqualTo(Category.JOYFUL)
        assertThat(draft.tempo).isEqualTo(Tempo(132))
        assertThat(draft.melody.notes).containsExactly(GridNote(pitch("E4"), 0, 4), GridNote(pitch("G4"), 6, 10))
        assertThat(draft.bass.notes).containsExactly(GridNote(pitch("C3"), 0, 16))
    }

    @Test
    fun `a score in three beats a bar, such as the Minuet, does not open`() {
        assertThat(ScoreDraft.fromScore(Scores.minuet, Scale(pitch("G4"), ScaleType.MAJOR), null)).isNull()
    }

    @Test
    fun `a score of other than eight bars does not open`() {
        // Given a score of nine bars
        val long = score(melody = thenSilence(notes("C4:4"), bars = 8), bass = thenSilence(notes("C3:4"), bars = 8))

        // Then
        assertThat(ScoreDraft.fromScore(long, cMajor, null)).isNull()
    }

    @Test
    fun `a score with a note shorter than a sixteenth or off the sixteenth grid, such as a triplet, does not open`() {
        // Given thirty-second notes, then a triplet of eighths
        val thirtySeconds = notes("C4:0.125 D4:0.125 E4:3.75")
        val triplet = listOf(1.0 / 3, 1.0 / 3, 1.0 / 3).map { Note(pitch("C4"), it) } + notes("D4:3")

        // Then
        assertThat(ScoreDraft.fromScore(score(thenSilence(thirtySeconds, 7), silence()), cMajor, null)).isNull()
        assertThat(ScoreDraft.fromScore(score(thenSilence(triplet, 7), silence()), cMajor, null)).isNull()
    }

    @Test
    fun `a score of a single voice opens as a melody without bass`() {
        // Given
        val melodyOnly = score(thenSilence(notes("E4:4"), bars = 7), silence()).let {
            it.copy(voices = it.voices.take(1))
        }

        // When
        val draft = ScoreDraft.fromScore(melodyOnly, cMajor, null)

        // Then
        assertThat(draft?.melody?.notes).containsExactly(GridNote(pitch("E4"), 0, 16))
        assertThat(draft?.bass?.notes).isEmpty()
    }

    @Test
    fun `a score of more than two voices does not open`() {
        // Given
        val threeVoices = score(silence(), silence()).let { it.copy(voices = it.voices + it.voices.first()) }

        // Then
        assertThat(ScoreDraft.fromScore(threeVoices, cMajor, null)).isNull()
    }

    @Test
    fun `a note out of the scale, such as a chromatic passing note, opens on a line of its own, out of the scale`() {
        // Given a Mysterious melody in D dorian that passes through C#5
        val dDorian = Scale(pitch("D4"), ScaleType.DORIAN)
        val mysterious = score(thenSilence(notes("D5:3 C#5:0.5 C5:0.5"), bars = 7), silence())

        // When
        val rows = ScoreDraft.fromScore(mysterious, dDorian, Category.MYSTERIOUS)!!.melody.rows

        // Then
        assertThat(rows).contains(GridRow(pitch("C#5"), inScale = false), GridRow(pitch("C5"), inScale = true))
        assertThat(rows.filterNot { it.inScale }).hasSize(1)
    }

    @Test
    fun `a note out of the range of its track opens on a line of its own, once`() {
        // Given a bass that goes up to E4, above the C2 to C4 range of the bass grid
        val high = score(silence(), thenSilence(notes("C3:2 E4:2"), bars = 7))

        // When
        val rows = ScoreDraft.fromScore(high, cMajor, null)!!.bass.rows

        // Then
        assertThat(rows).hasSize(16)
            .endsWith(GridRow(pitch("C4"), inScale = true), GridRow(pitch("E4"), inScale = true))
    }

    @ParameterizedTest
    @ValueSource(ints = [59, 201])
    fun `a score whose tempo is out of the tempos of the editor does not open`(bpm: Int) {
        assertThat(ScoreDraft.fromScore(score(silence(), silence(), bpm = bpm), cMajor, null)).isNull()
    }

    @Test
    fun `a score in two beats a bar, sixteen bars long, does not open`() {
        // Given the 32 beats of eight bars of four, written in sixteen bars of two
        val twoFour = score(silence(), silence()).copy(beatsPerBar = 2)

        // Then
        assertThat(ScoreDraft.fromScore(twoFour, cMajor, null)).isNull()
    }
}
