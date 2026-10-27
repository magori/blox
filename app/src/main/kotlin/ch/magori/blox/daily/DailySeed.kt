package ch.magori.blox.daily

import java.time.Clock
import java.time.LocalDate

/**
 * The seed of the Graine du jour: the date written `yyyyMMdd` (20261026 for the 26th of October 2026), so that every
 * player gets the same pieces on the same day, and a later day a greater seed. Same type as the seed of a game route.
 */
internal object DailySeed {
    fun of(date: LocalDate): Long = date.year * 10_000L + date.monthValue * 100 + date.dayOfMonth

    /**
     * The seed of today, the local date as the [clock] and its zone give it (injected for the tests). To get the date
     * of the phone, pass a clock in the zone of the system read at each call, `Clock.systemDefaultZone()`: never a
     * UTC clock, which gives the wrong day around midnight, nor one kept for the life of the process, which misses a
     * change of time zone.
     */
    fun today(clock: Clock): Long = of(LocalDate.now(clock))
}
