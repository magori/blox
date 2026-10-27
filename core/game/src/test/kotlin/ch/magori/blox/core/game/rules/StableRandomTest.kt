package ch.magori.blox.core.game.rules

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class StableRandomTest {
    @Test
    fun `the first numbers of seed 0 are the published SplitMix64 values`() {
        val random = StableRandom(seed = 0L)

        val numbers = List(5) { random.nextLong() }

        // Reference outputs of SplitMix64 (Steele, Lea, Flood, 2014) for the seed 0, computed outside this code.
        assertThat(numbers).containsExactly(
            0xE220A8397B1DCDAFuL.toLong(),
            0x6E789E6AA1B965F4uL.toLong(),
            0x06C45D188009454FuL.toLong(),
            0xF88BB8A8724C81ECuL.toLong(),
            0x1B39896A51A8749BuL.toLong(),
        )
    }

    @Test
    fun `the same seed gives the same numbers`() {
        val first = StableRandom(seed = 20261026L)
        val second = StableRandom(seed = 20261026L)

        assertThat(List(100) { first.nextLong() }).isEqualTo(List(100) { second.nextLong() })
    }

    @Test
    fun `bits are taken from the high end of the next number`() {
        // 0xE220A8397B1DCDAF is the first SplitMix64 number of the seed 0.
        assertThat(StableRandom(seed = 0L).nextBits(32)).isEqualTo(0xE220A839u.toInt())
        assertThat(StableRandom(seed = 0L).nextBits(3)).isEqualTo(0b111)
        assertThat(StableRandom(seed = 0L).nextBits(0)).isZero()
    }

    @Test
    fun `a number drawn in a range is the remainder of the high 31 bits of the next number`() {
        // High 31 bits of 0xE220A8397B1DCDAF: 0x7110541C = 1896895516, whose remainders are 4 (by 8) and 5 (by 7).
        assertThat(StableRandom(seed = 0L).nextInt(8)).isEqualTo(4)
        assertThat(StableRandom(seed = 0L).nextInt(7)).isEqualTo(5)
    }

    @ParameterizedTest(name = "until {0}")
    @ValueSource(ints = [1, 2, 7, 8, 100])
    fun `a number drawn in a range takes every value of the range`(until: Int) {
        val random = StableRandom(seed = 20261026L)

        val drawn = List(100 * until) { random.nextInt(until) }

        assertThat(drawn).allMatch { it in 0 until until }
        assertThat(drawn.toSet()).isEqualTo((0 until until).toSet())
    }

    @Test
    fun `a number drawn in a large range is not biased toward its low values`() {
        // 2^31 = 4/3 of this range: a plain remainder of 31 bits would give the values under 2^29 once in two draws
        // instead of once in three. The draws that would cause that bias are rejected.
        val until = 3 shl 29
        val random = StableRandom(seed = 20261026L)

        val lowShare = List(30_000) { random.nextInt(until) }.count { it < 1 shl 29 } / 30_000.0

        assertThat(lowShare).isBetween(0.32, 0.35)
    }

    @ParameterizedTest(name = "until {0}")
    @ValueSource(ints = [0, -1])
    fun `a number drawn in an empty range is refused`(until: Int) {
        assertThatIllegalArgumentException().isThrownBy { StableRandom(seed = 0L).nextInt(until) }
    }

    @ParameterizedTest(name = "bitCount {0}")
    @ValueSource(ints = [33, -1])
    fun `bits are drawn 0 to 32 at a time`(bitCount: Int) {
        assertThatIllegalArgumentException().isThrownBy { StableRandom(seed = 0L).nextBits(bitCount) }
    }

    @Test
    fun `a draw that would bias the range is replaced by the next number`() {
        // High 31 bits of the first number: 1896895516, beyond the last full run of 3 * 2^29 values in 2^31, so
        // rejected (its plain remainder would be 286282780). The second number gives 0x373C4F35 = 926699317.
        assertThat(StableRandom(seed = 0L).nextInt(3 shl 29)).isEqualTo(926699317)
        // The widest range has no incomplete run but one value, 2^31 - 1: 1896895516 is kept.
        assertThat(StableRandom(seed = 0L).nextInt(Int.MAX_VALUE)).isEqualTo(1896895516)
    }

    @Test
    fun `a number drawn between two bounds is the lower bound plus a draw in the width`() {
        // nextInt(8) of the seed 0 is 4.
        assertThat(StableRandom(seed = 0L).nextInt(10, 18)).isEqualTo(14)
    }

    @ParameterizedTest(name = "from {0} until {1}")
    @CsvSource("5, 5", "5, 4", "-2147483648, 0", "-1, 2147483647")
    fun `a number drawn between two bounds is refused when the range is empty or wider than the largest int`(
        from: Int,
        until: Int,
    ) {
        assertThatIllegalArgumentException().isThrownBy { StableRandom(seed = 0L).nextInt(from, until) }
    }
}
