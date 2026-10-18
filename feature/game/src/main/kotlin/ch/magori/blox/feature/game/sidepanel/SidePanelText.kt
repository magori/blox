package ch.magori.blox.feature.game.sidepanel

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.designsystem.text.scoreText as sharedScoreText

private const val LEVEL_DIGITS = 2
private const val LINES_DIGITS = 3

/** The score on 6 digits padded with zeros, capped at 999 999 as on the NES, as every screen shows it. */
internal fun scoreText(score: Points): String = sharedScoreText(score.value)

/** The level on at least 2 digits, padded with zeros; a higher level is shown in full. */
internal fun levelText(level: Level): String = digits(level.value, LEVEL_DIGITS)

/** The lines on at least 3 digits, padded with zeros; more lines are shown in full. */
internal fun linesText(lines: Lines): String = digits(lines.value, LINES_DIGITS)

private fun digits(
    value: Int,
    count: Int,
): String = value.toString().padStart(count, '0')
