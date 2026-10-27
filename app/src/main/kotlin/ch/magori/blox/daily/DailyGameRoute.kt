package ch.magori.blox.daily

import ch.magori.blox.feature.game.navigation.GameRoute
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/**
 * Today for the Graine du jour (docs/graine-du-jour.md): the local date of the phone as [clock] gives it, the only
 * reading of the date in `app`, for the entry and its game alike. The clock is the one of the zone of the system, read
 * at each call, by default: never a UTC clock, which gives the wrong day around midnight, nor one kept for the life of
 * the process, which misses a change of time zone.
 */
internal fun today(clock: Clock = Clock.systemDefaultZone()): LocalDate = LocalDate.now(clock)

/**
 * The key of a new daily game of [date], the date the entry shows, so that the game draws the pieces shown, even once
 * the phone passed midnight; the seed of that date, both kept in the route, so that a game started one day counts for
 * that day; a new id drawn from [random], so that each game gets its own key (ADR 0011).
 */
internal fun newDailyGameRoute(
    date: LocalDate,
    random: Random = Random.Default,
): GameRoute = GameRoute.daily(date, DailySeed.of(date), random.nextLong())
