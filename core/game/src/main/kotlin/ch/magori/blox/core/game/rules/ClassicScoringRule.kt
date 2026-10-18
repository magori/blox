package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level

/**
 * Scoring of the Classic mode (NES, 1989): a base value per number of lines cleared at once,
 * multiplied by the level plus one.
 *
 * The soft drop scores one point per row dropped. This is a deliberate simplification of the NES
 * computation, still marked to be checked in `docs/regles-mode-classique.md` (Lignes et score).
 */
public object ClassicScoringRule : ScoringRule {
    override fun pointsForLinesCleared(
        linesCleared: Int,
        level: Level,
    ): Int {
        require(linesCleared in 0..4) { "Between 0 and 4 lines can be cleared at once: $linesCleared" }
        return baseValue(linesCleared) * (level.value + 1)
    }

    override fun pointsForSoftDrop(rowsDropped: Int): Int {
        require(rowsDropped >= 0) { "Rows soft dropped cannot be negative: $rowsDropped" }
        return rowsDropped
    }

    private fun baseValue(linesCleared: Int): Int = when (linesCleared) {
        0 -> 0
        1 -> 40
        2 -> 100
        3 -> 300
        4 -> 1200
        else -> error("unreachable")
    }
}
