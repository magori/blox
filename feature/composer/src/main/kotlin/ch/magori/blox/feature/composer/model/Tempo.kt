package ch.magori.blox.feature.composer.model

/**
 * The tempo of a music of the editor, in [bpm] beats per minute, from [SLOWEST] to [FASTEST]: wider than the tempos of
 * every category (70 for the slowest Calm music, 170 for the fastest Retro one) and of the arranged scores, so that a
 * generated music opens with its own tempo and can still be slowed down or sped up.
 */
@JvmInline
internal value class Tempo(val bpm: Int) {
    init {
        require(bpm in SLOWEST..FASTEST) { "A tempo goes from $SLOWEST to $FASTEST beats per minute, was $bpm" }
    }

    /** The tempo [STEP] beats per minute faster, [FASTEST] at most. */
    fun faster(): Tempo = Tempo(minOf(bpm + STEP, FASTEST))

    /** The tempo [STEP] beats per minute slower, [SLOWEST] at least. */
    fun slower(): Tempo = Tempo(maxOf(bpm - STEP, SLOWEST))

    companion object {
        const val SLOWEST: Int = 60
        const val FASTEST: Int = 200

        /** Beats per minute added or taken away by one touch on the tempo buttons. */
        const val STEP: Int = 10
    }
}
