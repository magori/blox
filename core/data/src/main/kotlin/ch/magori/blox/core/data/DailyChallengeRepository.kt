package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The results of the daily challenge, kept on the device, apart from the records of the Classic: the tries, the
 * first try and the best of the date in progress ([DailyResults]). A try is told twice: when it starts
 * ([startTry]), then when it ends ([endTry]), finished or left. A game restored after the process died is the same
 * try: it is not started again, only ended.
 */
interface DailyChallengeRepository {
    /**
     * The results of the date in progress, null before any try. Their date may be older than today (no try yet
     * today), or newer: the clock of the phone went back, or the player travelled west.
     *
     * Never fails with an [java.io.IOException]: results that cannot be read give null, once, and the flow then
     * ends. Any other failure is a bug, and goes through to the collector: it is not to be caught.
     */
    val results: Flow<DailyResults?>

    /**
     * Counts a try of [date] (the date of its seed) as soon as it starts (Play): a try of the date in progress adds
     * one, a try of a newer date starts that date over, a try of an older date is ignored. Once per try: never for a
     * game restored after the process died.
     *
     * @throws java.io.IOException if the results cannot be written; any other failure is a bug.
     */
    suspend fun startTry(date: LocalDate)

    /**
     * Ends the try of [date] with its [points], when its game is over or left: the first try that ends gives the
     * first, any try may give the best; an end whose start was not kept counts as a whole try at a newer date only.
     * The result compares [points] with the best of [date] from before this end, as the records do; an ignored try
     * (an older date) beats nothing.
     *
     * @throws java.io.IOException if the results cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if [points] is negative, before anything is written.
     */
    suspend fun endTry(
        date: LocalDate,
        points: Int,
    ): RecordResult
}
