package ch.magori.blox.core.game.model

/**
 * The number of lines cleared at once by one piece, from 0 to 4.
 *
 * @throws IllegalArgumentException if [count] is not between 0 and 4.
 */
@JvmInline
public value class LineClear(public val count: Int) {
    init {
        require(count in 0..MAX_COUNT) { "Between 0 and $MAX_COUNT lines can be cleared at once: $count" }
    }

    /** Whether 4 lines were cleared at once, the most a piece can clear. */
    public val isFourLines: Boolean get() = count == MAX_COUNT

    private companion object {
        const val MAX_COUNT = 4
    }
}
