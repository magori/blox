package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.firstInReadingOrder
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.util.Locale

/**
 * Not a test: the measure of padlocks 1 to 100 that calibrates the weights of the rating (P7.29). It runs only when
 * asked, and writes its table to `build/padlock-measure.txt`:
 *
 * ```
 * BLOX_PADLOCK_MEASURE=true ./gradlew :core:locksmith:test --tests "*PadlockMeasure" --rerun
 * ```
 *
 * For each padlock: its pieces, the outline and the area of its hole, its solutions (5 at most) and the effort of an
 * exact cover that looks for them, in nodes visited; then the medians of each tier and of each family of the cycle.
 */
@EnabledIfEnvironmentVariable(named = "BLOX_PADLOCK_MEASURE", matches = "true")
class PadlockMeasure {
    @Test
    fun `measure padlocks 1 to 100`() {
        val report = buildString { appendTable("Padlock.ofLevel(n)", Padlock::ofLevel) }
        File("build/padlock-measure.txt").writeText(report)
        println(report)
    }

    private fun StringBuilder.appendTable(
        title: String,
        padlockOf: (Int) -> Padlock,
    ) {
        val start = System.nanoTime()
        val padlocks = LEVELS.associateWith(padlockOf)
        val millis = (System.nanoTime() - start) / 1_000_000
        val rows = padlocks.map { (level, padlock) -> measure(level, padlock) }
        appendLine("## $title, generated in $millis ms")
        appendLine("n | pieces | given | outline | area | out/area | solutions | nodes")
        rows.forEach { row ->
            appendLine(
                "${row.level} | ${row.pieces} | ${row.given} | ${row.outline} | ${row.area} | " +
                    "${decimal(row.outline.toDouble() / row.area)} | ${row.solutions} | ${row.nodes}",
            )
        }
        appendLine()
        appendLine("group | padlocks | median out/area | median solutions | unique | median nodes")
        rows.groupBy { group(it.level) }.toSortedMap().forEach { (group, measures) ->
            appendLine(
                "$group | ${measures.size} | ${decimal(median(measures.map { it.outline.toDouble() / it.area }))} | " +
                    "${median(measures.map { it.solutions.toDouble() })} | ${measures.count { it.solutions == 1 }} | " +
                    "${median(measures.map { it.nodes.toDouble() })}",
            )
        }
        appendLine()
    }

    private fun decimal(value: Double): String = "%.2f".format(Locale.ROOT, value)

    private fun measure(
        level: Int,
        padlock: Padlock,
    ): Measure {
        val hole = padlock.hole
        val search = Search()
        search.count(hole, padlock.givenPieces.groupingBy { it }.eachCount())
        return Measure(
            level = level,
            pieces = padlock.solution.size,
            given = padlock.givenPieces.joinToString(""),
            outline = hole.sumOf { cell -> cell.neighbours().count { it !in hole } },
            area = hole.size,
            solutions = search.solutions,
            nodes = search.nodes,
        )
    }

    private data class Measure(
        val level: Int,
        val pieces: Int,
        val given: String,
        val outline: Int,
        val area: Int,
        val solutions: Int,
        val nodes: Int,
    )

    /**
     * The search of [PadlockSolver.count], in the same order (first free cell in reading order, types in the order of
     * [PieceType], orientations in their Classic order), up to [PadlockGenerator.SOLUTIONS_COUNTED] solutions, which
     * also counts its nodes. Keep it in step with [PadlockSolver.count].
     */
    private class Search {
        var solutions = 0
            private set
        var nodes = 0
            private set

        fun count(
            free: Set<Position>,
            pieces: Map<PieceType, Int>,
        ) {
            nodes++
            if (free.isEmpty()) {
                solutions++
                return
            }
            val first = free.firstInReadingOrder()
            for (type in PieceType.entries.filter { (pieces[it] ?: 0) > 0 }) {
                for (orientation in ClassicRotationSystem.orientations(type)) {
                    val piece = Piece(type, orientation, first - orientation.cells.firstInReadingOrder())
                    if (!free.containsAll(piece.cells)) continue
                    count(free - piece.cells, pieces + (type to pieces.getValue(type) - 1))
                    if (solutions >= PadlockGenerator.SOLUTIONS_COUNTED) return
                }
            }
        }
    }

    private companion object {
        val LEVELS = 1..100

        fun group(level: Int): String = when (level) {
            in 1..3 -> "01-03 Apprenti"
            in 4..6 -> "04-06 Compagnon sinueux"
            in 7..12 -> "07-12 Compagnon mixte"
            in 13..20 -> "13-20 Artisan"
            in 21..30 -> "21-30 Maître"
            else -> "31+ cycle ${level % 5}"
        }

        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            val middle = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
        }
    }
}
