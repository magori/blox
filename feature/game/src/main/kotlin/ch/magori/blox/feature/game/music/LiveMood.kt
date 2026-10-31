package ch.magori.blox.feature.game.music

/**
 * The mood of the live music of a game (docs/musique-vivante.md): [CALM] while the stack is low, [TENSE] while it is in
 * danger. The card of the pause names it, « Vivante · calme » or « Vivante · tendue » (V37).
 */
internal enum class LiveMood {
    CALM,
    TENSE,
    ;

    internal companion object {
        /** The mood of a live game whose stack is [inDanger] or not. */
        fun of(inDanger: Boolean): LiveMood = if (inDanger) TENSE else CALM
    }
}
