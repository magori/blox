package ch.magori.blox.core.music

import kotlin.math.abs

/** A move of the melody, from the note struck [before] to the next one struck, [after] it. */
internal data class Move(val before: StruckNote, val after: StruckNote) {
    /** Semitones from [before] up to [after], negative when the melody goes down. */
    val semitones: Int get() = after.pitch.midi - before.pitch.midi
}

/** The moves from one note struck to the next, rests and repeats left out. */
internal val Melody.moves: List<Move>
    get() = struck.zipWithNext(::Move).filter { it.semitones != 0 }

/**
 * The share of the [moves] that are steps to a neighbouring note: the melody goes mostly by step, about 70 % of its
 * moves.
 */
internal fun Melody.stepShare(scale: Scale): Double = shareOf { scale.isStep(it.before.pitch, it.after.pitch) }

/** The share of the [moves] that leap by a perfect fifth, up or down. */
internal val Melody.fifthShare: Double get() = shareOf { abs(it.semitones) == PERFECT_FIFTH }

/** The share of the [moves] that go down. */
internal val Melody.descentShare: Double get() = shareOf { it.semitones < 0 }

/** The share of the [moves] between two notes of the chord in place under the second one, in [bars]. */
internal fun Melody.arpeggioShare(bars: List<Bar>): Double = shareOf { move ->
    val chord = bars[move.after.bar].chordAt(move.after.onset)
    move.before.pitch in chord && move.after.pitch in chord
}

private fun Melody.shareOf(predicate: (Move) -> Boolean): Double = moves.count(predicate).toDouble() / moves.size

/** The bars, counted from 0, where the highest note of the melody is struck, once for each time it is struck. */
internal val Melody.peakBars: List<Int>
    get() {
        val highest = struck.maxOf { it.pitch.midi }
        return struck.filter { it.pitch.midi == highest }.map { it.bar }
    }

/**
 * Whether [after] is a neighbour of [before]: another note, with no note of the scale between them. In a scale of seven
 * notes, a second spans a whole tone at most, so that the augmented second of the harmonic minor is a leap; the minor
 * third between two neighbouring degrees of a pentatonic scale is a step.
 */
internal fun Scale.isStep(
    before: Pitch,
    after: Pitch,
): Boolean {
    val between = (minOf(before.midi, after.midi) + 1 until maxOf(before.midi, after.midi)).map(::Pitch)
    val widest = if (type.offsets.size == SEVEN_NOTES) WHOLE_TONE else Int.MAX_VALUE
    return before != after && between.none { it in this } && abs(after.midi - before.midi) <= widest
}

/** The notes of a diatonic scale, whose seconds span a whole tone at most. */
private const val SEVEN_NOTES = 7
