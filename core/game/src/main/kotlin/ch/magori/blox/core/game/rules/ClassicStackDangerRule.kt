package ch.magori.blox.core.game.rules

/**
 * Stack danger of the Classic mode, with hysteresis so that the danger does not flicker: the stack
 * enters danger when its height reaches 15 rows, and leaves it only when its height falls to 12 rows
 * or less. Both thresholds are still to be tuned by playing.
 */
public object ClassicStackDangerRule : StackDangerRule {
    private const val ENTER_AT = 15
    private const val LEAVE_AT = 12

    /** The rows of the board: no stack is higher. */
    private const val MAX_STACK_HEIGHT = 20

    /** @throws IllegalArgumentException if [stackHeight] is not between 0 and 20. */
    override fun inDanger(
        wasInDanger: Boolean,
        stackHeight: Int,
    ): Boolean {
        require(stackHeight in 0..MAX_STACK_HEIGHT) { "A stack height is in 0..$MAX_STACK_HEIGHT: $stackHeight" }
        return if (wasInDanger) stackHeight > LEAVE_AT else stackHeight >= ENTER_AT
    }
}
