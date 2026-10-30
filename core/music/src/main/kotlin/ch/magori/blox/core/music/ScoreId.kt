package ch.magori.blox.core.music

/**
 * Identifies a score of BloX, without saying how it is called: each screen names it in its own words
 * (its strings), so that no displayed text lives in the domain. A score is either built in, one of the
 * arrangements of [Scores] ([KOROBEINIKI], [MINUET]), or [Personal], a music of the player or one composed by a game.
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
     * A music that is not one of the arrangements, known by its [key] ([key] is not blank). It is either:
     * - a music of the player, generated or written in the music editor: the saved musics give its [key], a stable
     *   identity that a rename leaves unchanged (a random UUID in the library of the device);
     * - or a music composed by a game and never kept, under a fixed [key] of its own: the live music of a game
     *   (`LiveMusic.CALM_ID`, `LiveMusic.TENSE_ID`), the score of a garden (`GARDEN_SCORE_ID`, `core:garden`), the
     *   accompaniment of Au tempo (`Accompaniment`).
     *
     * The two never meet: a game key is no UUID and the library only knows the musics it saved, so no game music
     * can be listed, renamed or chosen for the games; the sequencer knows a score by the whole [Score], not by its
     * identity alone. A new game music takes a fixed key of its own, never the one of another.
     */
    public data class Personal(public val key: String) : ScoreId {
        init {
            require(key.isNotBlank()) { "A personal score is known by a key that is not blank" }
        }
    }
}
