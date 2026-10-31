package ch.magori.blox.core.music

import kotlin.random.Random

/**
 * The two moods of the live music of a game, composed together by [compose]: the [calm] music, of [Category.CALM], and
 * the [tense] one, of [Category.EPIC], which takes over when the stack climbs. Both span the eight bars of a generated
 * music, so that one takes over from the other at the same rank, and play on the same tonic, each at its own tempo.
 * Two musics of different tonics are refused with an `IllegalArgumentException`; the categories and the length are those
 * that [compose] gives, which the constructor leaves to it.
 */
public data class LiveMusic(public val calm: GeneratedMusic, public val tense: GeneratedMusic) {
    init {
        require(calm.scale.tonic == tense.scale.tonic) {
            "The calm and the tense music share their tonic, were ${calm.scale.tonic} and ${tense.scale.tonic}"
        }
    }

    public companion object {
        /**
         * The two moods composed from [seed], the seed of the game: the same seed gives the same two musics. Each one is
         * composed from a seed of its own, drawn from [seed], the calm one first, so that the calm one does not depend
         * on the tense one; the tense one is composed on the tonic of the calm one. The call composes two musics, each
         * as costly as [MusicGenerator.generate]: make it off the main thread.
         */
        public fun compose(seed: Long): LiveMusic {
            val seeds = Random(seed)
            val calm = composeCalm(seeds)
            val tense = MusicGenerator.generate(Category.EPIC, seeds.nextLong(), TENSE_ID, calm.scale.tonic)
            return LiveMusic(calm, tense)
        }

        /**
         * The calm music of the live music of [seed], composed alone: the one [compose] gives, at the cost of one music
         * only. What the home screen plays to listen to the live music. Make it off the main thread, as [compose].
         */
        public fun composeCalm(seed: Long): GeneratedMusic = composeCalm(Random(seed))

        /** The calm music, from the first seed drawn from [seeds]. */
        private fun composeCalm(seeds: Random): GeneratedMusic =
            MusicGenerator.generate(Category.CALM, seeds.nextLong(), CALM_ID)

        /**
         * The identity of the calm music, the same from one game to the next, by which the music played is known. It
         * names no music of the library: a live music is composed for a game and never kept.
         */
        public val CALM_ID: ScoreId.Personal = ScoreId.Personal("live-calm")

        /** The identity of the tense music, as [CALM_ID] is the one of the calm music. */
        public val TENSE_ID: ScoreId.Personal = ScoreId.Personal("live-tense")
    }
}
