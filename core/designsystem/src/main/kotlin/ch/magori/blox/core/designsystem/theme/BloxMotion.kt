package ch.magori.blox.core.designsystem.theme

/**
 * The durations of the interface, in ticks of 1/60 s, as the game and its screens count (ADR 0006): the `motion-*`
 * tokens of docs/design-system/tokens.json, written in milliseconds, each rounded to the nearest tick
 * (MotionTokensTest). In reduced effects nothing moves: only fades of [reducedFade] at most are left.
 */
object BloxMotion {
    /** A released button comes back (`motion-press`, 100 ms); the press itself is at once. */
    val press: Int = 6

    /** The curtain of the pause, which hides the board or gives it back (`motion-curtain`, 200 ms). */
    val curtain: Int = 12

    /** A card (pause, end of a game) or a sheet (music) comes in (`motion-card`, 300 ms). */
    val card: Int = 18

    /** A sticker bounces, NOUVEAU RECORD (`motion-sticker`, 500 ms). */
    val sticker: Int = 30

    /** The sticker of the points rises and fades (`motion-pop`, 750 ms). */
    val pop: Int = 45

    /** The fade that takes the place of a move in reduced effects (`motion-reduced-fade`, 133 ms). */
    val reducedFade: Int = 8

    /** The notes of the column played bounce, in the musical garden, at each eighth (`motion-garden-bounce`, 100 ms). */
    val gardenBounce: Int = 6

    /**
     * The bubbles of a piece placed in the musical garden light up, a change of mood fades, the same in reduced effects
     * (`motion-garden-glow`, 133 ms).
     */
    val gardenGlow: Int = 8

    /** Au hasard, in the musical garden: the pieces fall one by one, this far apart (`motion-garden-fall-step`, 33 ms). */
    val gardenFallStep: Int = 2
}
