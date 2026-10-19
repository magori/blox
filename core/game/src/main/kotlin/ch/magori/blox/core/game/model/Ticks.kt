package ch.magori.blox.core.game.model

/**
 * A duration in ticks (1 tick = 1/60 s), at least 1: a rule never waits 0 ticks. It also counts down what is
 * left of a wait, such as a line clear or an entry delay: see [afterOneTick].
 */
@JvmInline
public value class Ticks(public val value: Int) {
    init {
        require(value >= 1) { "A duration lasts at least 1 tick: $value" }
    }

    /** What is left of this duration once one tick has passed: null when that tick ends it. */
    internal fun afterOneTick(): Ticks? = if (value > 1) Ticks(value - 1) else null
}
