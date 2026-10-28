package ch.magori.blox.core.data

import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.EOFException

/**
 * The encoding of a single garden, without the header of a file: the body of `garden.bin`, and of each garden of a
 * file holding several.
 */
class GardenEncodingTest {
    @Test
    fun `a garden is written as its mood, the count of its pieces then each piece, without the header of a file`() {
        // Given
        val buffer = Buffer()

        // When a single O in the middle, in the epic mood, is written
        buffer.writeGarden(ANOTHER_GARDEN)

        // Then
        assertThat(buffer.readByteArray()).isEqualTo(O_IN_THE_MIDDLE_EPIC)
    }

    @Test
    fun `gardens written one after the other are read back in turn, each one stopping after its last piece`() {
        // Given a garden of two pieces in the joyful mood, then the O in the middle in the epic mood
        val buffer = Buffer().apply {
            writeGarden(A_GARDEN)
            writeGarden(ANOTHER_GARDEN)
        }

        // When
        val first = buffer.readGarden()
        val second = buffer.readGarden()

        // Then
        assertThat(first).isEqualTo(A_GARDEN)
        assertThat(second).isEqualTo(ANOTHER_GARDEN)
        assertThat(buffer.exhausted()).isTrue()
    }

    @Test
    fun `a garden cut short is refused as cut short`() {
        // Given the O in the middle, its last four bytes missing
        val buffer = Buffer().write(O_IN_THE_MIDDLE_EPIC.copyOf(O_IN_THE_MIDDLE_EPIC.size - 4))

        // When / Then
        assertThatThrownBy { buffer.readGarden() }.isInstanceOf(EOFException::class.java)
    }

    @Test
    fun `a garden holding what no garden can be is refused as invalid`() {
        // Given a mood no version knows
        val buffer = Buffer().text("JAZZ").writeInt(0)

        // When / Then
        assertThatThrownBy { buffer.readGarden() }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a garden whose pieces overlap is refused as invalid`() {
        // Given the same O twice
        val buffer = Buffer().text("EPIC").writeInt(2).o(column = 8, row = 6).o(column = 8, row = 6)

        // When / Then
        assertThatThrownBy { buffer.readGarden() }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a garden whose piece is off the grid is refused as invalid`() {
        // Given an O whose left column would be -1
        val buffer = Buffer().text("EPIC").writeInt(1).o(column = 0, row = 6)

        // When / Then
        assertThatThrownBy { buffer.readGarden() }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = [-1, 57, Int.MAX_VALUE])
    fun `a garden counting fewer than none or more than 56 pieces is refused as invalid, before any piece is read`(
        pieces: Int,
    ) {
        // Given a count of pieces out of bounds, 56 pieces covering the 16 × 14 cells of the grid, and no piece after it
        val buffer = Buffer().text("EPIC").writeInt(pieces)

        // When / Then invalid, not cut short: the count is refused before the pieces are looked for
        assertThatThrownBy { buffer.readGarden() }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a garden whose name is longer than 64 bytes is refused as invalid, before its bytes are read`() {
        // Given a name of 65 bytes, cut short after its length
        val buffer = Buffer().writeInt(65)

        // When / Then
        assertThatThrownBy { buffer.readGarden() }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
