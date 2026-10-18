package ch.magori.blox.core.game.model

/** Game level, from 0 upwards. */
@JvmInline
public value class Level(public val value: Int) {
    init {
        require(value >= 0) { "A level cannot be negative: $value" }
    }
}
