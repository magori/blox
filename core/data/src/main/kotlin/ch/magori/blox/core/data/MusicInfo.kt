package ch.magori.blox.core.data

import ch.magori.blox.core.music.Score
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
