package ch.magori.blox.daily

import ch.magori.blox.WarmValue
import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.feature.cartridges.Cartridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The cartridges that bear the sticker NOUVELLE on the choice of the game (V36): the Graine du jour, until the first try
 * of [today] (docs/graine-du-jour.md, Cartouche), as its entry tells the results of the day apart. A try counts as soon
 * as it starts, so the results of today mean a try of today; results of another day, or none, leave it new.
 * `feature:cartridges` knows neither the results nor the date: `app` hands it [current] and [updates].
 *
 * The [results] are read once and kept warm in [scope] ([WarmValue]): the cartridges coming back know at once, from
 * their first frame, whether the sticker shows ([current]), without waiting for a collection. The date is read at each
 * call and at each update, never kept: a new day gives the sticker back when the cartridges show again.
 */
class NewCartridges(results: Flow<DailyResults?>, scope: CoroutineScope, private val today: () -> LocalDate) {
    /** The results, kept warm; their value may be null once read: no try kept. */
    private val read = WarmValue(results.map(::Read), scope)

    /** The cartridges new today, at each reading of the results, from the first. */
    val updates: Flow<Set<Cartridge>> = read.updates.map { newCartridgesOf(it.results, today()) }

    /** The cartridges new today, now; none before the results are read, so that no sticker shows that may have to go. */
    fun current(): Set<Cartridge> = read.current.value?.let { newCartridgesOf(it.results, today()) }.orEmpty()

    /** Results read, possibly none. */
    private class Read(val results: DailyResults?)
}

private fun newCartridgesOf(
    results: DailyResults?,
    today: LocalDate,
): Set<Cartridge> = if (results?.date == today) emptySet() else setOf(Cartridge.DAILY)
