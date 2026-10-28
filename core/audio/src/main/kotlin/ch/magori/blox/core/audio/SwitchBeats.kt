package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Voice
import kotlin.math.roundToInt

/**
 * The beats of [score], from 0, where the tempo may switch: those where a note starts in every voice.
 * A note that goes across a beat would be cut there, which clicks. Beat 0 is always one of them, the
 * start of the loop. A beat is compared exactly with the sums of the durations of each voice. Not every duration is
 * exact in [Double] (a third of an eighth, in a garden of `core:garden`): a voice whose rounding drifted would lose the
 * beats after it, so the scores write each note from where its voice stands, and their notes end exactly on the beats.
 */
internal fun switchBeats(score: Score): List<Int> {
    val beats = score.beatCount
    val starts = score.voices.map(::noteStarts)
    return (0 until beats).filter { beat -> starts.all { beat.toDouble() in it } }
}

/** The beats where the notes of [voice] start. */
private fun noteStarts(voice: Voice): Set<Double> =
    voice.notes.runningFold(0.0) { start, note -> start + note.beats }.toSet()

/** The number of beats of a score, the length of each of its voices: a whole number of bars. */
internal val Score.beatCount: Int
    get() = voices.first().beats.roundToInt()
