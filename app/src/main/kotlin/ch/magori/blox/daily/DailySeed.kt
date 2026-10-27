package ch.magori.blox.daily

import java.time.LocalDate

/**
 * The seed of the Graine du jour: the date written `yyyyMMdd` (20261026 for the 26th of October 2026), so that every
 * player gets the same pieces on the same day, and a later day a greater seed. Same type as the seed of a game route.
 * The date of today is read by [today], from the clock of the phone.
 */
internal object DailySeed {
    fun of(date: LocalDate): Long = date.year * 10_000L + date.monthValue * 100 + date.dayOfMonth
}
