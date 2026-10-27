package ch.magori.blox.feature.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The sizes of the padlock on the screen: 8 × 8 cells inside a thin rim of metal, in whole pixels. */
class PadlockSizesTest {
    @Test
    fun `the cells take their largest size when the width allows it`() {
        // When: a phone of 390 dp at 2.75 pixels per dp, 16 dp of margin on each side, cells of 42 dp at most
        val sizes = padlockSizes(width = 358 * 2.75f, maxCell = 42 * 2.75f)

        // Then: 115.5 pixels, cut down to a whole number
        assertThat(sizes.cell).isEqualTo(115)
    }

    @Test
    fun `on a narrow width, eight cells and the rim on both sides fit, in a whole number of pixels`() {
        // When
        val sizes = padlockSizes(width = 300f, maxCell = 1000f)

        // Then: 300 / 8.5 = 35.3
        assertThat(sizes.cell).isEqualTo(35)
        assertThat(8 * sizes.cell + 2 * sizes.rim).isLessThanOrEqualTo(300)
    }

    @Test
    fun `when the rim rounds up, eight cells and the rim still fit the width`() {
        // When: 935 / 8.5 = 110 pixels a cell, whose quarter, 27.5, rounds up to 28: 8 × 110 + 2 × 28 = 936
        val sizes = padlockSizes(width = 935f, maxCell = 1000f)

        // Then
        assertThat(8 * sizes.cell + 2 * sizes.rim).isLessThanOrEqualTo(935)
    }

    @Test
    fun `a cell of the preview on the ring overflows the rim by a cell less the rim, the room kept above and below`() {
        // Given: cells of 42 pixels in a rim of 11
        val sizes = PadlockSizes(cell = 42, rim = 11)

        // Then: a cell on the ring, drawn whole from the inner edge of the rim, passes its outer edge by 31 pixels
        assertThat(sizes.overflow).isEqualTo(31)
    }

    @Test
    fun `while the padlock is closed, the room kept is the overflow less the gap to the neighbours`() {
        assertThat(PadlockSizes(cell = 42, rim = 11).room(gap = 12, open = false)).isEqualTo(19)
        assertThat(PadlockSizes(cell = 42, rim = 11).room(gap = 40, open = false)).isEqualTo(0)
    }

    @Test
    fun `once the padlock is open, no preview is left and no room is kept`() {
        assertThat(PadlockSizes(cell = 42, rim = 11).room(gap = 12, open = true)).isEqualTo(0)
    }

    @Test
    fun `the rim is a quarter of a cell, rounded to a whole number of pixels`() {
        assertThat(padlockSizes(width = 300f, maxCell = 1000f).rim).isEqualTo(9)
        assertThat(padlockSizes(width = 1000f, maxCell = 42f).rim).isEqualTo(11)
    }
}
