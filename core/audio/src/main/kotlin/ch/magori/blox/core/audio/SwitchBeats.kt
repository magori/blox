package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Voice
import kotlin.math.roundToInt

/**
 * The beats of [score], from 0, where the tempo may switch: those where a note starts in every voice.
 * A note that goes across a beat would be cut there, which clicks. Beat 0 is always one of them, the
 * start of the loop. Durations are multiples of an eighth note, exact in [Double].
 */
internal fun switchBeats(score: Score): List<Int> {
    val beats = score.voices.first().beats.roundToInt()
    val starts = score.voices.map(::noteStarts)
    return (0 until beats).filter { beat -> starts.all { beat.toDouble() in it } }
}

/** The beats where the notes of [voice] start. */
private fun noteStarts(voice: Voice): Set<Double> =
    voice.notes.runningFold(0.0) { start, note -> start + note.beats }.toSet()
