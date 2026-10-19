package ch.magori.blox.core.music

import kotlin.random.Random

/**
 * The music generator: it composes [CANDIDATES] candidates of a category, rules out those without a melody or a bass,
 * notes the others by their [Quality], and keeps the best; while none reaches the threshold of its category, it composes
 * another round, [ROUNDS] at most, then keeps the best one it has, even under the threshold. Public as the entry point of
 * the button that rolls the dice in the music editor (P4.20); everything it composes with stays internal.
 */
public object MusicGenerator {
    /** The candidates composed in a round. */
    internal const val CANDIDATES: Int = 20

    /** The most rounds of candidates the generator composes. */
    internal const val ROUNDS: Int = 3

    /**
     * The music of [choice] composed from [seed], its score known by [id], the identity the music editor gives the
     * music it opens. The code of a music is the pair of its choice and its seed: the same choice and the same seed
     * give the same music, so that the pair finds it again. The call is costly, up to some
     * tens of milliseconds on the JVM and more on a phone, as it composes and notes twenty candidates or more: make it
     * off the main thread.
     *
     * @throws IllegalStateException if every candidate of every round is ruled out, sixty in a row, which breaks an
     * invariant of the generator rather than a chance of the draw: the most often ruled out, those of mysterious
     * without a bass, are about one in four.
     */
    public fun generate(
        choice: CategoryChoice,
        seed: Long,
        id: ScoreId.Personal,
    ): GeneratedMusic {
        val generation = choose(choice, seed)
        return assemble(generation, choice, seed, id)
    }

    /**
     * The music of the best candidate of [generation], composed from [choice] and [seed]: its melody and its bass on the
     * waves and at the volumes of the arrangements, known by [id], eight bars of four beats at the tempo of its form.
     */
    internal fun assemble(
        generation: Generation,
        choice: CategoryChoice,
        seed: Long,
        id: ScoreId.Personal,
    ): GeneratedMusic {
        val best = generation.best
        val score = Score(
            id = id,
            bpm = best.form.tempo,
            beatsPerBar = GENERATED_BEATS_PER_BAR,
            voices = listOf(melodyVoice(best.melody.bars.flatten()), bassVoice(best.bass.bars.flatten())),
        )
        return GeneratedMusic(score, generation.category, best.form.scale, choice, seed)
    }

    /**
     * The candidates of the category of [choice] composed from [seed], round after round while none reaches [threshold], by
     * default the threshold of the category, [ROUNDS] at most, and the best of them all. Surprise draws its category
     * first, once for all the candidates.
     */
    internal fun choose(
        choice: CategoryChoice,
        seed: Long,
        threshold: Double? = null,
    ): Generation {
        val master = Random(seed)
        val category = choice.resolve(master)
        val enough = threshold ?: category.quality.threshold
        val rounds = mutableListOf<List<Candidate>>()
        while (rounds.size < ROUNDS && rounds.flatten().none { it.quality.points >= enough }) {
            rounds += List(CANDIDATES) { Random(master.nextLong()) }.mapNotNull { random ->
                Candidate.compose(Form.compose(category, random), random)
            }
        }
        return Generation(category, rounds)
    }
}

/**
 * A music the generator composed: its [score], the [category] it plays, drawn by Surprise if need be, so that the
 * editor may propose a name after it, the [scale] the editor follows, and its code, the [choice] and the [seed] it was
 * composed from.
 */
public data class GeneratedMusic(
    public val score: Score,
    public val category: Category,
    public val scale: Scale,
    public val choice: CategoryChoice,
    public val seed: Long,
)

/**
 * The [rounds] of candidates of [category] a generation composed, those ruled out left out, and the [best] of them all,
 * the first one composed among those of equal points. Rounds left without a single candidate are refused with an
 * `IllegalStateException`: it would take every candidate of every round to be ruled out, sixty in a row, where the most
 * often ruled out, those of mysterious, are about one in four.
 */
internal data class Generation(val category: Category, val rounds: List<List<Candidate>>) {
    val best: Candidate = checkNotNull(rounds.flatten().maxByOrNull { it.quality.points }) {
        "Every candidate of the ${rounds.size} rounds was ruled out"
    }
}
