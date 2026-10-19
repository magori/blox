package ch.magori.blox.core.music

import kotlin.math.floor

/** A sound of [beats] beats in a rhythm, or a silence if [isRest]. */
public data class RhythmStep(public val beats: Double, public val isRest: Boolean = false) {
    init {
        requirePositiveBeats(beats, "A step")
    }
}

/** Beats in a bar of the music the generator composes; a [Score] gives its own in [Score.beatsPerBar]. */
public const val GENERATED_BEATS_PER_BAR: Int = 4

/**
 * The rhythm of one bar of melody, as [steps] of sound and silence, without pitches: they fill
 * [GENERATED_BEATS_PER_BAR].
 */
public data class RhythmCell(public val steps: List<RhythmStep>) {
    init {
        val beats = steps.sumOf { it.beats }
        require(beats == GENERATED_BEATS_PER_BAR.toDouble()) {
            "A cell fills one bar of $GENERATED_BEATS_PER_BAR beats, was $beats"
        }
    }

    private val spans: List<Span> get() =
        steps.zip(steps.runningFold(0.0) { onset, step -> onset + step.beats }) { step, onset -> Span(step, onset) }

    /** Whether the cell is silent on beat 1 or 3, the strong beats where the melody plays a chord note. */
    public val restsOnStrongBeat: Boolean
        get() = STRONG_BEATS.any { beat -> spans.any { it.step.isRest && it.covers(beat) } }

    /**
     * The notes struck off the strong beats, between two beats or on a weak beat, and held over the next beat: a
     * light rhythm holds one at most.
     */
    public val syncopations: Int get() = spans.count(Span::isSyncopation)

    /** Whether the cell ends on a long note, a half note at least, or on a silence: a phrase ending on it breathes. */
    internal val breathes: Boolean get() = steps.last().let { it.isRest || it.beats >= HALF_NOTE }

    /** A [step] struck at [onset] beats from the start of the bar. */
    private data class Span(val step: RhythmStep, val onset: Double) {
        private val end: Double get() = onset + step.beats

        fun covers(time: Double): Boolean = time >= onset && time < end

        /** A note struck off the strong beats, between two beats or on a weak one, and held past the next beat. */
        val isSyncopation: Boolean get() = !step.isRest && onset !in STRONG_BEATS && end > floor(onset) + 1
    }

    public companion object {
        private const val REST = "r"

        /** The shortest note a phrase may end on to breathe, in beats. */
        private const val HALF_NOTE = 2.0

        /** Strong beats 1 and 3, as times in beats from the start of the bar. */
        internal val STRONG_BEATS: List<Double> = listOf(0.0, 2.0)

        /** Reads durations in beats separated by spaces, a rest written with `r` before it: `"1.5 0.5 1 r1"`. */
        internal fun parse(steps: String): RhythmCell = RhythmCell(
            steps.split(" ").map { step ->
                RhythmStep(step.removePrefix(REST).toDouble(), isRest = step.startsWith(REST))
            },
        )
    }
}
