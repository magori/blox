package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.random.Random

class TempoRangeTest {
    @Test
    fun `a drawn tempo stays within the range and may be either end`() {
        // Given
        val range = TempoRange(slowest = 120, fastest = 123)

        // When
        val tempos = (0L until 200L).map { seed -> range.draw(Random(seed)) }.toSet()

        // Then
        assertThat(tempos).containsExactlyInAnyOrder(120, 121, 122, 123)
    }

    @ParameterizedTest(name = "{0} to {1}")
    @CsvSource("0, 90", "-10, 90", "150, 120")
    fun `a range that is not a positive tempo up to a faster or equal one is refused`(
        slowest: Int,
        fastest: Int,
    ) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { TempoRange(slowest, fastest) }
    }

    @Test
    fun `a range of a single tempo always draws that tempo`() {
        // Given
        val range = TempoRange(slowest = 100, fastest = 100)

        // When
        val tempos = (0L until 20L).map { seed -> range.draw(Random(seed)) }.toSet()

        // Then
        assertThat(tempos).containsExactly(100)
    }
}
