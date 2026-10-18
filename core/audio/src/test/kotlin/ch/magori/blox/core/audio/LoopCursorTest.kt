package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class LoopCursorTest {
    private val buffer = floatArrayOf(1f, 2f, 3f, 4f, 5f)

    @Test
    fun `slices follow one another from the start of the buffer`() {
        // Given
        val slice = FloatArray(2)

        // When
        val second = LoopCursor(buffer).fill(slice).also { assertThat(slice).containsExactly(1f, 2f) }.fill(slice)

        // Then
        assertThat(slice).containsExactly(3f, 4f)
        assertThat(second.position).isEqualTo(4)
    }

    @Test
    fun `a slice across the end of the buffer goes on from its start, without a gap`() {
        // Given a cursor on the fourth sample
        val cursor = LoopCursor(buffer).fill(FloatArray(3))
        val slice = FloatArray(3)

        // When
        val next = cursor.fill(slice)

        // Then
        assertThat(slice).containsExactly(4f, 5f, 1f)
        assertThat(next.position).isEqualTo(1)
    }

    @Test
    fun `a slice longer than the buffer goes round it as many times as needed`() {
        // Given
        val slice = FloatArray(12)

        // When
        val next = LoopCursor(buffer).fill(slice)

        // Then
        assertThat(slice).containsExactly(1f, 2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f, 1f, 2f)
        assertThat(next.position).isEqualTo(2)
    }

    @Test
    fun `an empty buffer is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { LoopCursor(FloatArray(0)) }
    }
}
