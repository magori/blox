package ch.magori.blox.core.music

import kotlin.math.sign

/** The notes of two voices sounding together in [bar], counted from 0, at [onset] beats: [upper] over [lower]. */
internal data class Simultaneity(val bar: Int, val onset: Double, val upper: Pitch, val lower: Pitch) {
    /** Semitones from [lower] up to [upper], octaves left out. */
    private val interval: Int get() = upper.semitonesAboveInOctave(lower)

    /** Whether the voices sound a perfect fifth, or an octave or unison, octaves left out, which may not move in parallel. */
    val isPerfect: Boolean get() = interval in PERFECT_INTERVALS

    /** Whether both voices sound other notes in [other] than in this one. */
    fun bothMovedIn(other: Simultaneity): Boolean = other.upper != upper && other.lower != lower

    /**
     * Whether the voices move in parallel fifths or octaves from these notes to [next]: a perfect fifth, or an octave or
     * unison, octaves left out, then the same interval again, both voices moving the same way. Contrary or oblique
     * motion, or the same notes again, are not parallel.
     */
    fun movesInParallelTo(next: Simultaneity): Boolean {
        val upperMove = (next.upper.midi - upper.midi).sign
        val lowerMove = (next.lower.midi - lower.midi).sign
        return isPerfect && next.interval == interval && upperMove != 0 && upperMove == lowerMove
    }

    private companion object {
        /** The intervals that may not move in parallel, octaves left out: the unison or octave, and the perfect fifth. */
        val PERFECT_INTERVALS = setOf(0, PERFECT_FIFTH)
    }
}

/** Two voices moving in parallel fifths or octaves from [before] to [after]. */
internal data class Parallel(val before: Simultaneity, val after: Simultaneity)

/**
 * The parallel fifths and octaves between the voices that strike [upper] and [lower], as the music loops: see
 * [verticalsOf] and [parallelsAmong].
 */
internal fun parallelsBetween(
    upper: List<StruckNote>,
    lower: List<StruckNote>,
): List<Parallel> = parallelsAmong(verticalsOf(upper, lower), loops = true)

/**
 * The notes of two voices sounding together at each note either of them strikes before [until] beats from the start
 * of the music, in the order they are played. Each voice, given by the notes it strikes in order, holds a note until
 * it strikes the next one; before its first note, it still holds its last one, as the music loops.
 */
internal fun verticalsOf(
    upper: List<StruckNote>,
    lower: List<StruckNote>,
    until: Double = Double.POSITIVE_INFINITY,
): List<Simultaneity> {
    if (upper.isEmpty() || lower.isEmpty()) return emptyList()
    val times = (upper + lower).map { it.bar to it.onset }.distinct()
        .filter { (bar, onset) -> timeOf(bar, onset) < until }
        .sortedBy { (bar, onset) -> timeOf(bar, onset) }
    var high = -1
    var low = -1
    return times.map { (bar, onset) ->
        val time = timeOf(bar, onset)
        while (high + 1 < upper.size && upper[high + 1].time <= time) high++
        while (low + 1 < lower.size && lower[low + 1].time <= time) low++
        Simultaneity(
            bar,
            onset,
            upper.getOrElse(high) {
                upper.last()
            }.pitch,
            lower.getOrElse(low) { lower.last() }.pitch,
        )
    }
}

/**
 * The parallel fifths and octaves among [verticals]: each one moves to the next one where both voices sound other
 * notes, whatever one voice plays while the other holds; from the last ones back to the first ones too if the music
 * [loops].
 */
internal fun parallelsAmong(
    verticals: List<Simultaneity>,
    loops: Boolean,
): List<Parallel> = verticals.indices.mapNotNull { index ->
    val vertical = verticals[index]
    val later = if (loops) {
        (1 until verticals.size).asSequence().map { verticals[(index + it) % verticals.size] }
    } else {
        verticals.asSequence().drop(index + 1)
    }
    later.firstOrNull(vertical::bothMovedIn)?.takeIf(vertical::movesInParallelTo)?.let { Parallel(vertical, it) }
}

/** Beats from the start of the music to [onset] in [bar], counted from 0. */
internal fun timeOf(
    bar: Int,
    onset: Double,
): Double = bar * GENERATED_BEATS_PER_BAR + onset

private val StruckNote.time: Double get() = timeOf(bar, onset)
