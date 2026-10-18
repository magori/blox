package ch.magori.blox.feature.game.sidepanel

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines

private const val SCORE_DIGITS = 6
private const val LEVEL_DIGITS = 2
private const val LINES_DIGITS = 3

/** The highest score the NES shows; the game itself keeps the real score. */
private const val MAX_SCORE_SHOWN = 999_999

/** The score on 6 digits padded with zeros, capped at 999 999 as on the NES. */
internal fun scoreText(score: Int): String = digits(score.coerceAtMost(MAX_SCORE_SHOWN), SCORE_DIGITS)

/** The level on at least 2 digits, padded with zeros; a higher level is shown in full. */
internal fun levelText(level: Level): String = digits(level.value, LEVEL_DIGITS)

/** The lines on at least 3 digits, padded with zeros; more lines are shown in full. */
internal fun linesText(lines: Lines): String = digits(lines.value, LINES_DIGITS)

private fun digits(
    value: Int,
    count: Int,
): String = value.toString().padStart(count, '0')
