package ch.magori.blox.core.data

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** What the cards of « Mes musiques » tell of a music, in French: its length. */
class MusicInfoTest {
    @ParameterizedTest(name = "8 bars at {0} beats a minute last {1}")
    @CsvSource("120, 0:16", "136, 0:14", "45, 0:43", "30, 1:04")
    fun `a music lasts its beats at its tempo, in minutes and seconds, rounded to the nearest second`(
        bpm: Int,
        length: String,
    ) {
        // Given a music of 8 bars of 4 beats
        val id = ScoreId.Personal("a")
        val score = Score(id, bpm, beatsPerBar = 4, voices = listOf(Voice(Waveform.SQUARE, 0.07, List(8) { WHOLE })))

        // When / Then
        assertThat(lengthText(score)).isEqualTo(length)
    }

    @Test
    fun `a music in bars of 3 beats lasts its own beats, not those of bars of 4`() {
        // Given 8 bars of 3 beats at 120 beats a minute: 24 beats
        val score =
            Score(
                ScoreId.Personal("a"),
                120,
                beatsPerBar = 3,
                voices = listOf(
                    Voice(
                        Waveform.SQUARE,
                        0.07,
                        List(8) {
                            DOTTED_HALF
                        },
                    ),
                ),
            )

        // When / Then
        assertThat(lengthText(score)).isEqualTo("0:12")
    }

    @Test
    fun `half a second is rounded up to the next second`() {
        // Given 5 bars of 5 beats at 120 beats a minute: 25 beats, 12.5 seconds
        val score =
            Score(
                ScoreId.Personal("a"),
                120,
                beatsPerBar = 5,
                voices = listOf(
                    Voice(
                        Waveform.SQUARE,
                        0.07,
                        List(5) {
                            FIVE_BEATS
                        },
                    ),
                ),
            )

        // When / Then
        assertThat(lengthText(score)).isEqualTo("0:13")
    }

    @ParameterizedTest(name = "8 bars at {0} beats a minute are said {1}")
    @CsvSource(
        "80, 24 secondes",
        "120, 16 secondes",
        "30, 1 minute 4 secondes",
        "32, 1 minute",
        "15, 2 minutes 8 secondes",
    )
    fun `a length is said in minutes and seconds, as TalkBack reads it`(
        bpm: Int,
        spoken: String,
    ) {
        // Given a music of 8 bars of 4 beats
        val score =
            Score(
                ScoreId.Personal("a"),
                bpm,
                beatsPerBar = 4,
                voices = listOf(
                    Voice(
                        Waveform.SQUARE,
                        0.07,
                        List(8) {
                            WHOLE
                        },
                    ),
                ),
            )

        // When / Then
        assertThat(spokenLength(score)).isEqualTo(spoken)
    }

    @Test
    fun `a length of one second is said in the singular`() {
        // Given a music of one bar of 4 beats at 240 beats a minute
        val score =
            Score(
                ScoreId.Personal("a"),
                240,
                beatsPerBar = 4,
                voices = listOf(Voice(Waveform.SQUARE, 0.07, listOf(WHOLE))),
            )

        // When / Then
        assertThat(spokenLength(score)).isEqualTo("1 seconde")
    }

    private companion object {

        /** A C4 held for a bar of 4 beats. */
        val WHOLE = Note(Pitch.parse("C4"), 4.0)

        /** A C4 held for a bar of 3 beats. */
        val DOTTED_HALF = Note(Pitch.parse("C4"), 3.0)

        /** A C4 held for a bar of 5 beats. */
        val FIVE_BEATS = Note(Pitch.parse("C4"), 5.0)
    }
}
