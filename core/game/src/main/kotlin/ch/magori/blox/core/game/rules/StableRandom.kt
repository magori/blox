package ch.magori.blox.core.game.rules

import kotlin.random.Random

/**
 * A random generator whose sequence is a pure function of its [seed], the same in every version of Kotlin and of
 * the application: `kotlin.random.Random(seed)` does not promise that (ADR 0026, which amends ADR 0014).
 *
 * The algorithm is SplitMix64 (Guy L. Steele Jr., Doug Lea and Christopher H. Flood, "Fast Splittable Pseudorandom
 * Number Generators", OOPSLA 2014), with its standard constants: the state grows by the increment
 * `0x9E3779B97F4A7C15` at each draw, then is mixed by xor-shifts of 30, 27 and 31 bits and the multipliers
 * `0xBF58476D1CE4E5B9` and `0x94D049BB133111EB`. With the seed 0, the first numbers are
 * `0xE220A8397B1DCDAF`, `0x6E789E6AA1B965F4`, `0x06C45D188009454F`…
 *
 * The contract covers the draws safe for a sequence that must last, such as the dice of the Garden (P10.7):
 * - [nextLong];
 * - [nextBits], the high bits of the next number;
 * - `nextInt(until)`, the remainder of the high 31 bits, rejected and drawn again when it would bias the result;
 * - `nextInt(from, until)`, `from` plus `nextInt(until - from)`;
 * - `List.random(random)` and `shuffle(random)`, which draw through `nextInt(until)`.
 *
 * The other draws of [Random] are built by the standard library on [nextBits], and are not part of the contract.
 */
public class StableRandom(seed: Long) : Random() {
    private var state: Long = seed

    override fun nextLong(): Long {
        state += INCREMENT
        var z = state
        z = (z xor (z ushr FIRST_SHIFT)) * FIRST_MULTIPLIER
        z = (z xor (z ushr SECOND_SHIFT)) * SECOND_MULTIPLIER
        return z xor (z ushr THIRD_SHIFT)
    }

    /** The [bitCount] high bits of the next number. */
    override fun nextBits(bitCount: Int): Int {
        require(bitCount in 0..Int.SIZE_BITS) { "Bits are drawn 0 to ${Int.SIZE_BITS} at a time, not $bitCount" }
        return if (bitCount == 0) 0 else (nextLong() ushr (Long.SIZE_BITS - bitCount)).toInt()
    }

    /**
     * A number in `0 until until`, without bias: the remainder of 31 random bits, drawn again when those bits fall
     * in the last incomplete run of `until` values, which would make the low values more likely.
     */
    override fun nextInt(until: Int): Int {
        require(until > 0) { "The range 0 until $until is empty" }
        while (true) {
            val bits = nextBits(Int.SIZE_BITS - 1)
            val value = bits % until
            val runEndOverflows = bits - value + (until - 1) < 0
            if (!runEndOverflows) return value
        }
    }

    /** A number in `from until until`: [from] plus a draw in the width of the range, which must fit in an [Int]. */
    override fun nextInt(
        from: Int,
        until: Int,
    ): Int {
        require(from < until) { "The range $from until $until is empty" }
        val width = until.toLong() - from
        require(width <= Int.MAX_VALUE) { "The range $from until $until is wider than ${Int.MAX_VALUE}" }
        return from + nextInt(width.toInt())
    }

    private companion object {
        const val INCREMENT: Long = -0x61C8864680B583EBL // 0x9E3779B97F4A7C15
        const val FIRST_MULTIPLIER: Long = -0x40A7B892E31B1A47L // 0xBF58476D1CE4E5B9
        const val SECOND_MULTIPLIER: Long = -0x6B2FB644ECCEEE15L // 0x94D049BB133111EB
        const val FIRST_SHIFT = 30
        const val SECOND_SHIFT = 27
        const val THIRD_SHIFT = 31
    }
}
