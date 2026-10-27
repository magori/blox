package ch.magori.blox.feature.daily

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The month in letters, in French as the whole application. */
private val month: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH)

/**
 * [date] in letters, without its year: « 26 octobre », the name of the Graine du jour (« Graine du 26 octobre »); the
 * first of the month « 1er novembre », as French writes it.
 */
internal fun dayInLetters(date: LocalDate): String {
    val day = if (date.dayOfMonth == 1) "1er" else "${date.dayOfMonth}"
    return "$day ${month.format(date)}"
}
