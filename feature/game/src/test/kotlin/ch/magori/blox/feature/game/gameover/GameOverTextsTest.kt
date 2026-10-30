package ch.magori.blox.feature.game.gameover

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GameOverTextsTest {
    @ParameterizedTest(name = "{0} ticks -> {1}")
    @CsvSource("0, 00:00", "3599, 00:59", "3600, 01:00", "216000, 60:00", "6000000, 1666:40")
    fun `the duration is shown as minutes and seconds from the ticks played`(
        ticks: Int,
        expected: String,
    ) {
        assertThat(durationText(ticks)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} ticks -> {1} min {2} s")
    @CsvSource("0, 0, 0", "3599, 0, 59", "3900, 1, 5", "216000, 60, 0")
    fun `the duration is split into whole minutes and seconds`(
        ticks: Int,
        minutes: Int,
        seconds: Int,
    ) {
        assertThat(durationParts(ticks)).isEqualTo(DurationParts(minutes, seconds))
    }

    @ParameterizedTest(name = "{0} min {1} s -> minutes {2}, seconds {3}")
    @CsvSource("1, 5, 1, 5", "1, 0, 1, ", "0, 5, , 5", "0, 0, 0, ")
    fun `TalkBack says the minutes and the seconds, never 0 seconds, and 0 minutes only for an empty duration`(
        minutes: Int,
        seconds: Int,
        spokenMinutes: Int?,
        spokenSeconds: Int?,
    ) {
        assertThat(
            durationSpeech(DurationParts(minutes, seconds)),
        ).isEqualTo(DurationSpeech(spokenMinutes, spokenSeconds))
    }

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource("1200, 001200", "1234567, 999999")
    fun `the final score is shown as in the game, capped at 999 999`(
        score: Int,
        expected: String,
    ) {
        assertThat(textsFor(score = score).score).isEqualTo(expected)
    }

    @Test
    fun `the lines, level, four-line clears and duration are shown as in the game`() {
        val texts = textsFor(lines = 123, level = 19, fourLineClears = 7, ticksPlayed = 3600)

        assertThat(texts.lines).isEqualTo("123")
        assertThat(texts.level).isEqualTo("19")
        assertThat(texts.fourLineClears).isEqualTo("7")
        assertThat(texts.duration).isEqualTo("01:00")
    }

    @Test
    fun `the lines and level keep their padding zeros, as in the game`() {
        val texts = textsFor(lines = 7, level = 5)

        assertThat(texts.lines).isEqualTo("007")
        assertThat(texts.level).isEqualTo("05")
    }

    @Test
    fun `TalkBack reads the raw values, without padding zeros, and the duration in parts`() {
        val texts = textsFor(score = 1200, lines = 7, level = 5, fourLineClears = 3, ticksPlayed = 3900)

        assertThat(texts.spokenScore).isEqualTo("1200")
        assertThat(texts.spokenLines).isEqualTo("7")
        assertThat(texts.spokenLevel).isEqualTo("5")
        assertThat(texts.spokenFourLineClears).isEqualTo("3")
        assertThat(texts.spokenDuration).isEqualTo(DurationSpeech(minutes = 1, seconds = 5))
    }

    @ParameterizedTest(name = "best before {0} -> {1}, read {2}")
    @CsvSource("98760, 098760, 98760", "1234567, 999999, 1234567")
    fun `the best before the game is shown as a score, capped at 999 999, and read at its real value`(
        previousBest: Int,
        shown: String,
        spoken: String,
    ) {
        assertThat(textsFor(previousBest = previousBest).previousBest).isEqualTo(PreviousBestTexts(shown, spoken))
    }

    @Test
    fun `without a best before the game no best is recalled`() {
        assertThat(textsFor(previousBest = 0).previousBest).isNull()
    }

    @Test
    fun `when the results kept do not hold the game no best is recalled`() {
        assertThat(textsFor(previousBest = 98_760, resultsKept = false).previousBest).isNull()
    }

    @Test
    fun `a practice game over cannot keep its results`() {
        // Given / When a game over told practice, whose try was not counted, but said to keep its results
        // Then it is refused: a try not counted is not kept, and Copier would copy results without it
        assertThatThrownBy {
            GameOverUiState(
                score = Points(0),
                lines = Lines(0),
                level = Level(0),
                fourLineClears = 0,
                ticksPlayed = 0,
                newBest = false,
                resultsKept = true,
                previousBest = 0,
                practice = true,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    /** When: the game over screen gets these values; other values do not matter to the test. */
    private fun textsFor(
        score: Int = 0,
        lines: Int = 0,
        level: Int = 0,
        fourLineClears: Int = 0,
        ticksPlayed: Int = 0,
        previousBest: Int = 0,
        resultsKept: Boolean = true,
    ): GameOverTexts = gameOverTexts(
        GameOverUiState(
            score = Points(score),
            lines = Lines(lines),
            level = Level(level),
            fourLineClears = fourLineClears,
            ticksPlayed = ticksPlayed,
            newBest = false,
            resultsKept = resultsKept,
            previousBest = previousBest,
            practice = false,
        ),
    )
}
