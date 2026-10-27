package ch.magori.blox.core.data

import java.time.LocalDate

/**
 * The results of the daily challenge for the date in progress: only that date is kept, no history.
 *
 * @property date the date of the tries: the date of the seed of their game (its route), not the date of the phone
 *   when they start or end; a game replayed after midnight keeps the date of its seed.
 * @property tries the tries of [date], each counted as soon as it starts, those under way included.
 * @property firstTry the points of the first try of [date]: 0 until it ends.
 * @property best the best points of the tries of [date] that ended: 0 before any.
 * @property firstTryOver whether [firstTry] is final: the first try ended, or a second try started (the first one
 *   can no longer be restored, and stays at what it was worth).
 */
data class DailyResults(
    val date: LocalDate,
    val tries: Int,
    val firstTry: Int = 0,
    val best: Int = 0,
    val firstTryOver: Boolean = false,
) {
    /** [firstTry] once it is final ([firstTryOver]); null while the first try may still change, shown « — ». */
    val finalFirstTry: Int? get() = firstTry.takeIf { firstTryOver }

    /** [best] once a try ended with points or the first try is over; null before, shown « — ». */
    val knownBest: Int? get() = best.takeIf { firstTryOver || best > 0 }
}

/*
 * The rules of every DailyChallengeRepository, the DataStore one and the fake of the test fixtures alike: what a
 * start or an end does to the results kept. Not an API to call: the repositories apply them in their updates.
 */

/**
 * [this] results (null: none) once a try of [date] has started: a try of the date in progress is counted, a try of
 * a newer date starts that date over, a try of an older date is ignored.
 */
internal fun DailyResults?.afterStart(date: LocalDate): DailyResults = when {
    this == null || date > this.date -> DailyResults(date = date, tries = 1)
    date < this.date -> this
    else -> copy(tries = tries + 1, firstTryOver = true)
}

/**
 * [this] results (null: none) once a try of [date] has ended with [points], not negative: the first try that ends
 * gives the first, and every try may give the best. An end without its start (one that could not be written) is
 * not the same at every date: at the date in progress it adds no try, its start being taken as counted; at a newer
 * date, or without results, it counts as a whole try, the first of that date. A try of an older date is ignored.
 */
internal fun DailyResults?.afterEnd(
    date: LocalDate,
    points: Int,
): DailyResults = when {
    this == null || date > this.date -> afterStart(date).afterEnd(date, points)
    date < this.date -> this
    firstTryOver -> copy(best = maxOf(best, points))
    else -> copy(firstTry = points, best = maxOf(best, points), firstTryOver = true)
}

/**
 * What a try of [date] ending with [points] does to the best of its date, from [this] results kept before it:
 * the previous best is that of [date] (0 for a newer date, or without results), and an ignored try beats nothing.
 */
internal fun DailyResults?.recordOf(
    date: LocalDate,
    points: Int,
): RecordResult = when {
    this == null || date > this.date -> RecordResult(previous = 0, isNewRecord = points > 0)
    date < this.date -> RecordResult(previous = best, isNewRecord = false)
    else -> RecordResult(previous = best, isNewRecord = points > best)
}
