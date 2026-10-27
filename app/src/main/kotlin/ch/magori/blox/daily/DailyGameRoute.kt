package ch.magori.blox.daily

import ch.magori.blox.feature.game.navigation.GameRoute
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/**
 * The key of a new daily game (docs/graine-du-jour.md): the local date of the phone as [clock] gives it, and the seed of
 * that date, both kept in the route, so that a game started one day counts for that day; a new id drawn from [random],
 * so that each game gets its own key (ADR 0011). The clock is the one of the zone of the system, read at each Jouer, by
 * default: never a UTC clock, which gives the wrong day around midnight, nor one kept for the life of the process,
 * which misses a change of time zone.
 */
internal fun newDailyGameRoute(
    clock: Clock = Clock.systemDefaultZone(),
    random: Random = Random.Default,
): GameRoute {
    val date = LocalDate.now(clock)
    return GameRoute.daily(date, DailySeed.of(date), random.nextLong())
}
