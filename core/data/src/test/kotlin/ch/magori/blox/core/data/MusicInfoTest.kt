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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** What the cards of « Mes musiques » tell of a music, in French: its length, its last change. */
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

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource(
        "2026-01-15, 15\u00A0janv.",
        "2026-02-15, 15\u00A0févr.",
        "2026-03-15, 15\u00A0mars",
        "2026-04-15, 15\u00A0avr.",
        "2026-05-15, 15\u00A0mai",
        "2026-06-15, 15\u00A0juin",
        "2026-07-15, 15\u00A0juil.",
        "2026-08-15, 15\u00A0août",
        "2026-09-15, 15\u00A0sept.",
        "2026-10-20, 20\u00A0oct.",
        "2026-11-15, 15\u00A0nov.",
        "2026-12-31, 31\u00A0déc.",
    )
    fun `a day is its number then its month shortened in French, which a line never parts`(
        day: String,
        text: String,
    ) {
        // Given noon of that day in Zurich
        val instant = LocalDate.parse(day).atTime(12, 0).atZone(ZURICH).toInstant()

        // When / Then
        assertThat(dayText(instant, ZURICH, TODAY)).isEqualTo(text)
    }

    @Test
    fun `the first day of a month is written 1er, as French writes it`() {
        // Given
        val instant = LocalDate.parse("2026-05-01").atTime(12, 0).atZone(ZURICH).toInstant()

        // When / Then
        assertThat(dayText(instant, ZURICH, TODAY)).isEqualTo("1er\u00A0mai")
    }

    @Test
    fun `the day is the one of the zone of the player, not the one of Greenwich`() {
        // Given half past eleven at night in Greenwich, half past one in the morning of the next day in Zurich
        val instant = Instant.parse("2026-10-20T23:30:00Z")

        // When / Then
        assertThat(dayText(instant, ZURICH, TODAY)).isEqualTo("21\u00A0oct.")
        assertThat(dayText(instant, ZoneOffset.UTC, TODAY)).isEqualTo("20\u00A0oct.")
    }

    @Test
    fun `a day of another year than today is written with its year`() {
        // Given a music changed on 20 October 2025, looked at in 2026
        val instant = Instant.parse("2025-10-20T12:00:00Z")

        // When / Then
        assertThat(dayText(instant, ZoneOffset.UTC, TODAY)).isEqualTo("20\u00A0oct.\u00A02025")
    }

    @Test
    fun `a day of the year that just ended takes its year on the first of January`() {
        // Given a music changed on 31 December 2026
        val instant = Instant.parse("2026-12-31T12:00:00Z")

        // When / Then the same day, then the next one, the first of a new year
        assertThat(dayText(instant, ZoneOffset.UTC, LocalDate.parse("2026-12-31"))).isEqualTo("31\u00A0d\u00E9c.")
        assertThat(dayText(instant, ZoneOffset.UTC, LocalDate.parse("2027-01-01")))
            .isEqualTo("31\u00A0d\u00E9c.\u00A02026")
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

    @ParameterizedTest(name = "{0} is said {1}")
    @CsvSource(
        "2026-01-15, 15 janvier",
        "2026-02-15, 15 février",
        "2026-03-15, 15 mars",
        "2026-04-15, 15 avril",
        "2026-05-01, 1er mai",
        "2026-06-15, 15 juin",
        "2026-07-15, 15 juillet",
        "2026-08-15, 15 août",
        "2026-09-15, 15 septembre",
        "2026-10-18, 18 octobre",
        "2026-11-15, 15 novembre",
        "2026-12-31, 31 décembre",
        "2025-10-20, 20 octobre 2025",
    )
    fun `a day is said with its month whole, and its year when it is not the year of today`(
        day: String,
        spoken: String,
    ) {
        // Given noon of that day in Zurich
        val instant = LocalDate.parse(day).atTime(12, 0).atZone(ZURICH).toInstant()

        // When / Then
        assertThat(spokenDay(instant, ZURICH, TODAY)).isEqualTo(spoken)
    }

    private companion object {
        val ZURICH: ZoneId = ZoneId.of("Europe/Zurich")

        /** The day the card is looked at, in the year of the days above. */
        val TODAY: LocalDate = LocalDate.parse("2026-10-20")

        /** A C4 held for a bar of 4 beats. */
        val WHOLE = Note(Pitch.parse("C4"), 4.0)

        /** A C4 held for a bar of 3 beats. */
        val DOTTED_HALF = Note(Pitch.parse("C4"), 3.0)

        /** A C4 held for a bar of 5 beats. */
        val FIVE_BEATS = Note(Pitch.parse("C4"), 5.0)
    }
}
