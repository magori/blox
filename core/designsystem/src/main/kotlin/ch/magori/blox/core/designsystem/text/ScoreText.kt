package ch.magori.blox.core.designsystem.text

private const val SCORE_DIGITS = 6

/** The highest score the NES shows; the game itself keeps the real score, which TalkBack reads. */
private const val MAX_SCORE_SHOWN = 999_999

/**
 * A score on 6 digits padded with zeros, capped at 999 999 as on the NES. Shared by the game and home
 * screens.
 *
 * @throws IllegalArgumentException if [score] is negative.
 */
fun scoreText(score: Int): String {
    require(score >= 0) { "A score cannot be negative: $score" }
    return score.coerceAtMost(MAX_SCORE_SHOWN).toString().padStart(SCORE_DIGITS, '0')
}
