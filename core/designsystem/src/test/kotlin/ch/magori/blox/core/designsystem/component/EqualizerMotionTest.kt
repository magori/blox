package ch.magori.blox.core.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class EqualizerMotionTest {
    @Test
    fun `the equalizer moves its three bars between 35 and 100 percent of their height in 30 ticks`() {
        // When over two rounds
        val parts = (0..120).map { tick -> equalizerParts(tick) }

        // Then three bars, never under 35 % nor over 100 %
        assertThat(parts).allSatisfy { bars ->
            assertThat(bars).hasSize(3)
            assertThat(bars).allSatisfy { part -> assertThat(part).isBetween(0.35f, 1f) }
        }
        // And the first one from 35 % up to 100 % in 30 ticks, back down in as many
        val first = parts.map { it[0] }
        assertThat(first[0]).isEqualTo(0.35f)
        assertThat(first[15]).isCloseTo(0.675f, within(1e-6f))
        assertThat(first[30]).isEqualTo(1f)
        assertThat(first[60]).isEqualTo(0.35f)
        assertThat(first.subList(0, 31)).isSorted()
        // And the others the same, each 10 ticks ahead of the one before: rising stairs at the start
        assertThat(parts[0]).containsExactly(first[0], first[10], first[20])
        assertThat(parts[25]).containsExactly(first[25], first[35], first[45])
    }
}
