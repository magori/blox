package ch.magori.blox.core.data

import ch.magori.blox.core.music.Score
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/**
 * How long [score] lasts at its tempo, in minutes and seconds, rounded to the nearest second: « 0:16 » for 8 bars of 4
 * beats at 120 beats a minute.
 */
fun lengthText(score: Score): String {
    val seconds = seconds(score)
    return "%d:%02d".format(Locale.ROOT, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

/**
 * How long [score] lasts, as TalkBack says it: « 24 secondes », « 1 minute 4 secondes », « 1 minute ». The same
 * length as [lengthText], rounded the same way.
 */
fun spokenLength(score: Score): String {
    val seconds = seconds(score)
    val minutes = seconds / SECONDS_PER_MINUTE
    val rest = seconds % SECONDS_PER_MINUTE
    return listOfNotNull(
        counted(minutes, "minute").takeIf { minutes > 0 },
        counted(rest, "seconde").takeIf { rest > 0 || minutes == 0 },
    ).joinToString(" ")
}

/** The seconds [score] lasts at its tempo, rounded to the nearest one, half a second up. */
private fun seconds(score: Score): Int = (score.voices.first().beats * SECONDS_PER_MINUTE / score.bpm).roundToInt()

/** [count] [unit], in the plural from 2 on, as French counts: « 1 minute », « 4 secondes ». */
private fun counted(
    count: Int,
    unit: String,
): String = if (count < 2) "$count $unit" else "$count ${unit}s"

private const val SECONDS_PER_MINUTE = 60

/**
 * The day of [instant] in [zone], its number then its month shortened in French: « 20 oct. », « 1er mai », followed by
 * its year when it is not the year of [today]: « 20 oct. 2025 ». No-break spaces join them, so that a line never parts
 * them. The months are written here, not by the formats of the system: the card says the same on every device, and in
 * the tests.
 */
fun dayText(
    instant: Instant,
    zone: ZoneId,
    today: LocalDate,
): String = day(instant, zone, today, SHORT_MONTHS, NO_BREAK_SPACE)

/** The day of [instant] as TalkBack says it, its month whole: « 18 octobre », « 1er mai », « 20 octobre 2025 ». */
fun spokenDay(
    instant: Instant,
    zone: ZoneId,
    today: LocalDate,
): String = day(instant, zone, today, MONTHS, ' ')

/**
 * The day of [instant] in [zone]: its number (« 1er » for the first), its month among [months], and its year when it
 * is not the year of [today], joined by [space].
 */
private fun day(
    instant: Instant,
    zone: ZoneId,
    today: LocalDate,
    months: List<String>,
    space: Char,
): String {
    val day = instant.atZone(zone).toLocalDate()
    val number = if (day.dayOfMonth == 1) "1er" else day.dayOfMonth.toString()
    val year = day.year.takeIf { it != today.year }
    return listOfNotNull(number, months[day.monthValue - 1], year).joinToString(space.toString())
}

private const val NO_BREAK_SPACE = '\u00A0'

/** The months shortened, as French writes them. */
private val SHORT_MONTHS = listOf(
    "janv.",
    "févr.",
    "mars",
    "avr.",
    "mai",
    "juin",
    "juil.",
    "août",
    "sept.",
    "oct.",
    "nov.",
    "déc.",
)

/** The months, whole. */
private val MONTHS = listOf(
    "janvier",
    "février",
    "mars",
    "avril",
    "mai",
    "juin",
    "juillet",
    "août",
    "septembre",
    "octobre",
    "novembre",
    "décembre",
)
