package ch.magori.blox.core.music

import kotlin.math.abs

/** The measures of a candidate: those of its [melody] and of its [bass], and what its category expects, [profile]. */
internal class CandidateMeasures(val profile: QualityProfile, val melody: MelodyMeasures, val bass: BassMeasures) {
    /** How far the melody leans as its category does: in full for a category that leans no way. */
    val leaning: Double
        get() = profile.leaning?.let { leaningFulfilment(it.lean.shareOf(melody), it.target) } ?: 1.0
}

/**
 * A rule of writing that the quality of a candidate music notes, worth [weight] points out of 100, met as far as
 * [fulfilment] reads it off the measures of the candidate, from 0 to 1.
 */
internal enum class Criterion(val weight: Int, private val fulfilment: (CandidateMeasures) -> Double) {
    /** The melody goes mostly by step, as much as its category aims at. */
    STEPS(25, { stepsFulfilment(it.melody.stepShare, it.profile.idealStepShare) }),

    /** The notes out of the chord are reached and left by step, each one that is not costing a share of the points. */
    PREPARED_NOTES(25, { preparedFulfilment(it.melody.unpreparedNotes) }),

    /** The bars taking up the motif keep its contour. */
    MOTIF(15, { it.melody.motifShare }),

    /** The bass goes from chord to chord by a fourth at most. */
    SMOOTH_BASS(15, { it.bass.smoothShare }),

    /**
     * The melody leans as the traits of its category do, by fifths, down or by arpeggios: in full for a category that
     * leans no way.
     */
    LEANING(10, { it.leaning }),

    /** The melody spans an octave to a major tenth. */
    SPAN(5, { spanFulfilment(it.melody.span) }),

    /** The melody arches up to its highest note, once, in bar 5 or 6. */
    ARCH(5, { archFulfilment(it.melody.peakBars) }),
    ;

    /** How far a candidate of [measures] meets the criterion, from 0 to 1. */
    fun fulfilmentOf(measures: CandidateMeasures): Double = fulfilment(measures)
}

/** The quality of a candidate music: how far it meets each criterion, from 0 to 1, in [fulfilment]. */
internal data class Quality(val fulfilment: Map<Criterion, Double>) {
    init {
        require(fulfilment.keys == Criterion.entries.toSet()) { "A quality notes every criterion, was $fulfilment" }
        require(fulfilment.values.all { it in 0.0..1.0 }) { "A criterion is met from 0 to 1, was $fulfilment" }
    }

    /** The points of the candidate out of 100: the weight of each criterion, as far as it is met. */
    val points: Double get() = Criterion.entries.sumOf { it.weight * fulfilment.getValue(it) }

    companion object {
        /** The quality of the music of [form], played by [melody] over [bass]. */
        fun of(
            form: Form,
            melody: Melody,
            bass: Bass,
        ): Quality {
            val measures =
                CandidateMeasures(form.category.quality, MelodyMeasures(form, melody), BassMeasures(form, bass))
            return Quality(Criterion.entries.associateWith { it.fulfilmentOf(measures) })
        }
    }
}

/**
 * How far a melody making [share] of its moves by step goes by step as much as its category aims at, [ideal]: in full at
 * the ideal share, not at all 30 points away.
 */
internal fun stepsFulfilment(
    share: Double,
    ideal: Double,
): Double = (1 - abs(share - ideal) / STEP_SHARE_TOLERANCE).coerceIn(0.0, 1.0)

/** How far from the ideal share the share of steps no longer meets the criterion at all. */
private const val STEP_SHARE_TOLERANCE = 0.3

/**
 * How far a melody spanning [semitones] keeps to a span between an octave and a major tenth: in full within it, not at
 * all a perfect fourth further, below or above.
 */
internal fun spanFulfilment(semitones: Int): Double {
    val outside = maxOf(SEMITONES_PER_OCTAVE - semitones, semitones - MAJOR_TENTH, 0)
    return (1 - outside.toDouble() / PERFECT_FOURTH).coerceIn(0.0, 1.0)
}

/**
 * Whether a melody whose highest note is struck in [peakBars], counted from 0, arches up to it: once, in bar 5 or 6, two
 * thirds into the music. A guard rather than a measure: the melody arches by construction, unless its search for an
 * arch gives up.
 */
internal fun archFulfilment(peakBars: List<Int>): Double = if (peakBars.singleOrNull() in ARCH_PEAK_BARS) 1.0 else 0.0

/** Bars 5 and 6, counted from 0, where the melody reaches its highest note. */
private val ARCH_PEAK_BARS = setOf(4, 5)

/**
 * How far a melody with [unprepared] notes out of the chord, not reached and left by step, prepares them: each one costs
 * the same share, bounded, so that a melody of many such notes is not worse than not at all.
 */
internal fun preparedFulfilment(unprepared: Int): Double =
    (1 - unprepared.toDouble() / MOST_UNPREPARED_NOTES).coerceAtLeast(0.0)

/** The unprepared notes from which the criterion is not met at all: the 90th percentile of dance, 13 for joyful. */
private const val MOST_UNPREPARED_NOTES = 16

/**
 * How far a melody making [share] of its moves the way its category leans meets the [target] share: in full from it, in
 * proportion below it, so that the choice of the best candidate keeps what the traits of the category build.
 */
internal fun leaningFulfilment(
    share: Double,
    target: Double,
): Double = (share / target).coerceAtMost(1.0)
