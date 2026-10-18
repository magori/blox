package ch.magori.blox.core.game.model

/** A duration in ticks (1 tick = 1/60 s), at least 1: a rule never waits 0 ticks. */
@JvmInline
public value class Ticks(public val value: Int) {
    init {
        require(value >= 1) { "A duration lasts at least 1 tick: $value" }
    }
}
