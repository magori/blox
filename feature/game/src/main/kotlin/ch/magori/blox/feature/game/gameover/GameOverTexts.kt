package ch.magori.blox.feature.game.gameover

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.frame.TICKS_PER_SECOND
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.sidepanel.levelText
import ch.magori.blox.feature.game.sidepanel.linesText
import ch.magori.blox.feature.game.sidepanel.scoreText

private const val SECONDS_PER_MINUTE = 60

/**
 * What the game over screen gets: the final values of the game, whether they make a [newBest], and whether the
 * results kept hold this game ([resultsKept]): the records written, for the Classic; for a daily game, its try written
 * under its date, neither failed to write nor ignored as a try of a date already passed. Otherwise a daily game over
 * does not offer to copy them: Copier would copy results without this game, or nothing.
 */
@Immutable
internal data class GameOverUiState(
    val score: Points,
    val lines: Lines,
    val level: Level,
    val fourLineClears: Int,
    val ticksPlayed: Int,
    val newBest: Boolean,
    val resultsKept: Boolean,
)

/**
 * The texts of the game over screen: those shown, formatted as in the game, and those read by
 * TalkBack, the raw values without padding zeros.
 */
@Immutable
internal data class GameOverTexts(
    val score: String,
    val lines: String,
    val level: String,
    val fourLineClears: String,
    val duration: String,
    val spokenScore: String,
    val spokenLines: String,
    val spokenLevel: String,
    val spokenFourLineClears: String,
    val spokenDuration: DurationSpeech,
)

/**
 * The texts of the game over screen for [state]: the score, lines and level as in the side column,
 * the number of four-line clears, and the duration of the ticks played as `mm:ss`; TalkBack reads
 * the raw values and the duration as a [DurationSpeech].
 */
internal fun gameOverTexts(state: GameOverUiState): GameOverTexts = GameOverTexts(
    score = scoreText(state.score),
    lines = linesText(state.lines),
    level = levelText(state.level),
    fourLineClears = state.fourLineClears.toString(),
    duration = durationText(state.ticksPlayed),
    spokenScore = state.score.value.toString(),
    spokenLines = state.lines.value.toString(),
    spokenLevel = state.level.value.toString(),
    spokenFourLineClears = state.fourLineClears.toString(),
    spokenDuration = durationSpeech(durationParts(state.ticksPlayed)),
)

/** A duration in whole [minutes] and the [seconds] left, 0 to 59. */
internal data class DurationParts(val minutes: Int, val seconds: Int)

/**
 * The duration of [ticks] ticks of play, in whole minutes and seconds. It is play time: ticks are
 * capped per frame and the pause is excluded, so it is not real time. Minutes are not turned into hours.
 */
internal fun durationParts(ticks: Int): DurationParts {
    val totalSeconds = ticks / TICKS_PER_SECOND
    return DurationParts(minutes = totalSeconds / SECONDS_PER_MINUTE, seconds = totalSeconds % SECONDS_PER_MINUTE)
}

/** The duration of [ticks] ticks of play as `mm:ss`, from [durationParts]; minutes beyond 99 take more digits. */
internal fun durationText(ticks: Int): String {
    val (minutes, seconds) = durationParts(ticks)
    return "${twoDigits(minutes)}:${twoDigits(seconds)}"
}

/** At least 2 digits, padded with a zero, whatever the language of the device. */
private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

/** What TalkBack says of a duration: the [minutes], the [seconds], or both; null is left unsaid. */
internal data class DurationSpeech(val minutes: Int?, val seconds: Int?)

/**
 * What TalkBack says of [parts]: the minutes and the seconds, without "0 seconds", and without
 * "0 minutes" unless the duration is empty.
 */
internal fun durationSpeech(parts: DurationParts): DurationSpeech = DurationSpeech(
    minutes = parts.minutes.takeIf { it > 0 || parts.seconds == 0 },
    seconds = parts.seconds.takeIf { it > 0 },
)
