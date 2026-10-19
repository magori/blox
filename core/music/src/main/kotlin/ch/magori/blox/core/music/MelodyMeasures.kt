package ch.magori.blox.core.music

import kotlin.math.abs
import kotlin.math.sign

/** A move of the melody, from the note struck [before] to the next one struck, [after] it. */
internal data class Move(val before: StruckNote, val after: StruckNote) {
    /** Semitones from [before] up to [after], negative when the melody goes down. */
    val semitones: Int get() = after.pitch.midi - before.pitch.midi
}

/** The moves from one note struck to the next, rests and repeats left out. */
internal val Melody.moves: List<Move>
    get() = struck.zipWithNext(::Move).filter { it.semitones != 0 }

/**
 * The measures of [melody] over its [form], from which the quality of a music is noted. A share is a number from 0 to
 * 1, never one that is not a number: a share of the moves is 0 for a melody that never moves, as it makes none, and a
 * share of the notes kept right is 1 when there is none to get wrong.
 */
internal class MelodyMeasures(private val form: Form, private val melody: Melody) {
    private val struck: List<StruckNote> = melody.struck

    private val moves: List<Move> = melody.moves

    /** The share of the moves that are steps to a neighbouring note: the melody goes mostly by step, about 70 %. */
    val stepShare: Double get() = moves.shareOf(ifNone = 0.0) { form.scale.isStep(it.before.pitch, it.after.pitch) }

    /** The share of the moves that leap by a perfect fifth, up or down. */
    val fifthShare: Double get() = moves.shareOf(ifNone = 0.0) { abs(it.semitones) == PERFECT_FIFTH }

    /** The share of the moves that go down. */
    val descentShare: Double get() = moves.shareOf(ifNone = 0.0) { it.semitones < 0 }

    /** The share of the moves between two notes of the chord in place under the second one. */
    val arpeggioShare: Double
        get() = moves.shareOf(ifNone = 0.0) { move ->
            val chord = form.chordUnder(move.after)
            move.before.pitch in chord && move.after.pitch in chord
        }

    /**
     * The bars, counted from 0, where the highest note of the melody is struck, once for each time it is struck: none
     * for a melody that strikes no note.
     */
    val peakBars: List<Int>
        get() {
            val highest = struck.maxOfOrNull { it.pitch.midi }
            return struck.filter { it.pitch.midi == highest }.map { it.bar }
        }

    /** Semitones from the lowest note struck to the highest: none for a melody that strikes no note. */
    val span: Int
        get() {
            val pitches = struck.map { it.pitch.midi }
            return (pitches.maxOrNull() ?: 0) - (pitches.minOrNull() ?: 0)
        }

    /**
     * The notes struck out of the chord in place that are not prepared, reached by step from the last other note before
     * and left by step to the next other note, as a passing note or a neighbour, struck again in between or not, from bar
     * 8 back to bar 1 too as the music loops.
     */
    val unpreparedNotes: Int
        get() = struck.indices.count { index ->
            val note = struck[index].pitch
            val before = nextOther(index, step = -1)
            val after = nextOther(index, step = 1)
            note !in form.chordUnder(struck[index]) &&
                !(before != null && after != null && form.scale.isStep(before, note) && form.scale.isStep(note, after))
        }

    /**
     * The share of the moves of the bars taking up the motif, after bar 1 which states it, that follow the moves of the
     * motif in turn, counted in degrees of the scale: the contour of the motif kept as it comes back on other chords.
     */
    val motifShare: Double
        get() {
            val contours = melody.bars.map(::contourOf)
            return form.bars.indices.drop(1).filter { form.bars[it].role == BarRole.MOTIF }.flatMap { bar ->
                contours[bar].zip(contours.first()) { move, motif -> move == motif }
            }.shareOf(ifNone = 1.0) { it }
        }

    /**
     * The first pitch other than the one struck at [index], going from it by [step], 1 forward or -1 back, round the loop:
     * `null` for a melody that strikes a single pitch.
     */
    private fun nextOther(
        index: Int,
        step: Int,
    ): Pitch? = (1 until struck.size).map { struck[(index + it * step).mod(struck.size)].pitch }
        .firstOrNull { it != struck[index].pitch }

    /** The moves from each note struck in [bar] to the next, in degrees of the scale: +1 a step up, 0 a repeat. */
    private fun contourOf(bar: List<Note>): List<Int> = bar.mapNotNull { it.pitch }.zipWithNext { before, after ->
        val between = minOf(before.midi, after.midi) + 1..maxOf(before.midi, after.midi)
        between.count { Pitch(it) in form.scale } * (after.midi - before.midi).sign
    }
}

/** The share of the elements that [predicate] holds for, or [ifNone] when there is none. */
internal fun <T> List<T>.shareOf(
    ifNone: Double,
    predicate: (T) -> Boolean,
): Double = if (isEmpty()) ifNone else count(predicate).toDouble() / size

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
