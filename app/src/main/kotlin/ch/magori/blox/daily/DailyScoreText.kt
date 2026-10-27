package ch.magori.blox.daily

import ch.magori.blox.core.data.DailyResults

/**
 * The texts of the copied score, from the resources of the application (`daily_score_text`, `daily_score_tries`):
 * [score] puts together the seed, the first try, the best and the tries, each already written; [tries] writes a number
 * of tries, its count written as [written], in the plural its count asks for. Given as functions, so that the text
 * stays a pure function, tested on the JVM with the texts of `strings.xml`.
 */
internal class DailyScoreTexts(
    val score: (seed: String, firstTry: String, best: String, tries: String) -> String,
    val tries: (count: Int, written: String) -> String,
)

/**
 * The text « Copier mon score » puts in the clipboard (docs/graine-du-jour.md), short enough for a message:
 * `BloX 20261026 · 1er 9 800 · meilleur 18 200 (5 essais)` in the [texts] of the application, the date of [results] as
 * its seed, the first try, the best and the tries of that date. The numbers are written whole, grouped by three, never
 * capped as on the screen; as the entry, a first try not over yet, and a best before any try ended, are written « — ».
 */
internal fun dailyScoreText(
    results: DailyResults,
    texts: DailyScoreTexts,
): String = texts.score(
    DailySeed.of(results.date).toString(),
    grouped(results.finalFirstTry),
    grouped(results.knownBest),
    texts.tries(results.tries, grouped(results.tries)),
)

/** [count] written whole, its digits grouped by three with a narrow no-break space (U+202F); null gives « — ». */
private fun grouped(count: Int?): String =
    count?.toString()?.reversed()?.chunked(3)?.joinToString("\u202F")?.reversed() ?: "—"
