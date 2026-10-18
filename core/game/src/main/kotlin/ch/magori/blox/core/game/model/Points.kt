package ch.magori.blox.core.game.model

/** Points earned in a game, never negative. */
@JvmInline
public value class Points(public val value: Int) {
    init {
        require(value >= 0) { "Points cannot be negative: $value" }
    }

    /** These points plus [other]. */
    public operator fun plus(other: Points): Points = Points(value + other.value)
}
