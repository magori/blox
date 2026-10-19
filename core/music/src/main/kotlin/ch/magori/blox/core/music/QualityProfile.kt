package ch.magori.blox.core.music

/**
 * What the note of quality expects of a category: [idealStepShare], the share of its moves its melodies make by step
 * with the traits of the category, measured over the seeds, [leaning], the moves its traits lean to, if any, so that
 * the choice of the best candidate does not undo what the traits build, and [threshold], the points the best candidate
 * of a round reaches for the generator to stop there: about the 15th percentile of the best of a first round, so that a
 * second round makes up for the weakest draws of each category.
 */
internal data class QualityProfile(val idealStepShare: Double, val threshold: Double, val leaning: Leaning? = null) {
    init {
        require(idealStepShare in 0.0..1.0) { "A share of steps is from 0 to 1, was $idealStepShare" }
    }
}

/** The moves a melody leans to, [lean], and the [target] share of its moves they make for the leaning to be met in full. */
internal data class Leaning(val lean: Lean, val target: Double) {
    init {
        require(target > 0 && target <= 1) { "A target share is above 0 and up to 1, was $target" }
    }
}

/** The kind of moves the traits of a category lean its melody to, each read off its own share of the moves. */
internal enum class Lean(private val share: (MelodyMeasures) -> Double) {
    FIFTHS({ it.fifthShare }),
    DESCENTS({ it.descentShare }),
    ARPEGGIOS({ it.arpeggioShare }),
    ;

    /** The share of the moves measured by [measures] that go this way. */
    fun shareOf(measures: MelodyMeasures): Double = share(measures)
}
