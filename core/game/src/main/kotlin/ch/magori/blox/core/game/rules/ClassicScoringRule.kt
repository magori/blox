package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Points

/**
 * Scoring of the Classic mode (NES, 1989): a base value per number of lines cleared at once,
 * multiplied by the level plus one.
 *
 * The soft drop scores one point per row dropped. This is a deliberate simplification of the NES
 * computation, still marked to be checked in `docs/regles-mode-classique.md` (Lignes et score).
 */
public object ClassicScoringRule : ScoringRule {
    override fun pointsForLinesCleared(
        lineClear: LineClear,
        level: Level,
    ): Points = Points(baseValue(lineClear) * (level.value + 1))

    override fun pointsForSoftDrop(rowsDropped: Int): Points {
        require(rowsDropped >= 0) { "Rows soft dropped cannot be negative: $rowsDropped" }
        return Points(rowsDropped)
    }

    private fun baseValue(lineClear: LineClear): Int = when (lineClear.count) {
        0 -> 0
        1 -> 40
        2 -> 100
        3 -> 300
        4 -> 1200
        else -> error("unreachable: LineClear holds 0 to 4 lines")
    }
}
