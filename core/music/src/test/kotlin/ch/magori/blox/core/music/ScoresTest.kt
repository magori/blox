package ch.magori.blox.core.music

import ch.magori.blox.core.music.Waveform.SQUARE
import ch.magori.blox.core.music.Waveform.TRIANGLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class ScoresTest {
    @Test
    fun `Korobeiniki is played at 150 bpm in 4-4 with a square melody at 0_07 and a triangle bass at 0_16`() {
        // Given
        val score = Scores.korobeiniki

        // When / Then
        assertThat(score.bpm).isEqualTo(150)
        assertThat(score.beatsPerBar).isEqualTo(4)
        assertThat(score.voices.map { it.waveform to it.volume }).containsExactly(SQUARE to 0.07, TRIANGLE to 0.16)
    }

    @Test
    fun `the minuet is played at 132 bpm in 3-4 with a square melody at 0_07 and a triangle bass at 0_16`() {
        // Given
        val score = Scores.minuet

        // When / Then
        assertThat(score.bpm).isEqualTo(132)
        assertThat(score.beatsPerBar).isEqualTo(3)
        assertThat(score.voices.map { it.waveform to it.volume }).containsExactly(SQUARE to 0.07, TRIANGLE to 0.16)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("scores")
    fun `in every voice no note crosses a bar line and every bar is complete`(score: Score) {
        score.voices.forEach { voice ->
            // When
            val bars = barsOf(voice, score.beatsPerBar)

            // Then: a note crossing a bar line makes its bar too long, an incomplete bar is too short
            assertThat(bars.map { bar -> bar.sumOf { it.beats } })
                .describedAs("beats of each bar of the ${voice.waveform} voice")
                .containsOnly(score.beatsPerBar.toDouble())
        }
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("scores")
    fun `every voice has 8 bars`(score: Score) {
        score.voices.forEach { voice ->
            // When / Then
            assertThat(barsOf(voice, score.beatsPerBar)).describedAs("bars of the ${voice.waveform} voice").hasSize(8)
        }
    }

    @Test
    fun `the first bar of the Korobeiniki melody is the one of docs musique md`() {
        // When
        val firstBar = barsOf(Scores.korobeiniki.voices.first(), beatsPerBar = 4).first()

        // Then
        assertThat(firstBar).containsExactly(
            note("E5", 1.0),
            note("B4", 0.5),
            note("C5", 0.5),
            note("D5", 1.0),
            note("C5", 0.5),
            note("B4", 0.5),
        )
    }

    @Test
    fun `the first bar of the minuet melody is the one of docs musique md`() {
        // When
        val firstBar = barsOf(Scores.minuet.voices.first(), beatsPerBar = 3).first()

        // Then
        assertThat(firstBar).containsExactly(
            note("D5", 1.0),
            note("G4", 0.5),
            note("A4", 0.5),
            note("B4", 0.5),
            note("C5", 0.5),
        )
    }

    @Test
    fun `the Korobeiniki bass alternates a root and its fifth, octave ignored`() {
        // Given
        val bass = Scores.korobeiniki.voices.single { it.waveform == TRIANGLE }

        barsOf(bass, beatsPerBar = 4).forEach { bar ->
            assertThat(bar).describedAs("bar $bar: four notes").hasSize(4)
            assertThat(bar).describedAs("bar $bar: no rest").noneMatch { it.isRest }

            // When
            val (root, fifth, rootAgain, fifthAgain) = bar.map { it.pitch!! }

            // Then
            assertThat(listOf(rootAgain, fifthAgain)).describedAs("bar $bar").containsExactly(root, fifth)
            assertThat((fifth.midi - root.midi).mod(12)).describedAs("bar $bar").isEqualTo(7)
        }
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("melodyKeys")
    fun `a melody only uses the pitch classes of its key`(
        melody: Voice,
        pitchClasses: Set<String>,
    ) {
        // When
        val outOfKey = pitchesOf(melody).filter { it.toString().dropLast(1) !in pitchClasses }

        // Then
        assertThat(outOfKey).describedAs("pitches outside $pitchClasses").isEmpty()
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("voiceRanges")
    fun `every voice stays within its range`(
        voice: Voice,
        lowest: String,
        highest: String,
    ) {
        // Given
        val range = Pitch.parse(lowest).midi..Pitch.parse(highest).midi

        // When
        val outOfRange = pitchesOf(voice).filter { it.midi !in range }

        // Then
        assertThat(outOfRange).describedAs("pitches outside $lowest..$highest").isEmpty()
    }

    private fun pitchesOf(voice: Voice): List<Pitch> = voice.notes.mapNotNull { it.pitch }

    private fun note(
        pitch: String,
        beats: Double,
    ): Note = Note(Pitch.parse(pitch), beats)

    /** Cuts [voice] into bars: a new bar starts once the current one holds [beatsPerBar] beats or more. */
    private fun barsOf(
        voice: Voice,
        beatsPerBar: Int,
    ): List<List<Note>> {
        val bars = mutableListOf(mutableListOf<Note>())
        voice.notes.forEach { note ->
            if (bars.last().sumOf { it.beats } >= beatsPerBar) bars += mutableListOf<Note>()
            bars.last() += note
        }
        return bars
    }

    private companion object {
        @JvmStatic
        fun scores(): List<Arguments> = listOf(
            argumentSet("Korobeiniki", Scores.korobeiniki),
            argumentSet("minuet", Scores.minuet),
        )

        @JvmStatic
        fun melodyKeys(): List<Arguments> = listOf(
            argumentSet(
                "Korobeiniki in A natural minor",
                Scores.korobeiniki.voices.first(),
                setOf("A", "B", "C", "D", "E", "F", "G"),
            ),
            argumentSet("minuet in G major", Scores.minuet.voices.first(), setOf("G", "A", "B", "C", "D", "E", "F#")),
        )

        @JvmStatic
        fun voiceRanges(): List<Arguments> = listOf(
            argumentSet("Korobeiniki melody", Scores.korobeiniki.voices.first(), "A4", "A5"),
            argumentSet("minuet melody", Scores.minuet.voices.first(), "F#4", "G5"),
            argumentSet("Korobeiniki bass", Scores.korobeiniki.voices.last(), "C2", "E3"),
            argumentSet("minuet bass", Scores.minuet.voices.last(), "C2", "E3"),
        )
    }
}
