package ch.magori.blox.core.game.rules

/**
 * Stack danger policy: whether the stack is high enough to be in danger, for instance to speed up
 * the music. The policy is interchangeable: [ClassicStackDangerRule] holds the Classic thresholds.
 */
public interface StackDangerRule {
    /**
     * Whether the stack is in danger once it reaches [stackHeight] rows, given whether it [wasInDanger]
     * before. A pure function: the caller keeps the previous answer.
     *
     * @throws IllegalArgumentException if [stackHeight] is not a height the board allows.
     */
    public fun inDanger(
        wasInDanger: Boolean,
        stackHeight: Int,
    ): Boolean
}
