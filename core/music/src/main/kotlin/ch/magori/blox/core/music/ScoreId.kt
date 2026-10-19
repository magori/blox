package ch.magori.blox.core.music

/**
 * Identifies a score of BloX, without saying how it is called: each screen names it in its own words
 * (its strings), so that no displayed text lives in the domain. A score is either built in, one of the
 * arrangements of [Scores] ([KOROBEINIKI], [MINUET]), or [Personal], a music of the player.
 *
 * The built-in identities are objects named as the constants they replace, so that `ScoreId.KOROBEINIKI`
 * still reads as one value among a closed few.
 */
public sealed interface ScoreId {
    /** The arrangement of Korobeiniki, [Scores.korobeiniki]. */
    public data object KOROBEINIKI : ScoreId

    /** The arrangement of the minuet, [Scores.minuet]. */
    public data object MINUET : ScoreId

    /**
     * A music of the player, generated or written in the music editor, known by its [key]: a stable identity
     * that a rename leaves unchanged. The saved musics give it ([key] is not blank).
     */
    public data class Personal(public val key: String) : ScoreId {
        init {
            require(key.isNotBlank()) { "A personal score is known by a key that is not blank" }
        }
    }
}
