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

    @Test
    fun `at 390 dp, a cell of the preview on the ring at the side is cut 4 dp from the edge, its cross whole within`() {
        // Given: one pixel per dp, 16 dp of margin on each side, cells of 42 dp in a rim of 11; 16 dp beyond the rim
        val sizes = padlockSizes(width = 358f, maxCell = 42f)

        // When
        val side = sizes.sideCell(beyond = 16, margin = 4)

        // Then the cell keeps 42 dp; the one on the ring is cut 4 dp from the edge: its 11 dp under the rim and 12 more
        assertThat(sizes.cell).isEqualTo(42)
        assertThat(side.width).isEqualTo(23)
        // Its cross, 60 % of its smaller side, whole within the part shown
        assertThat(side.cross).isEqualTo(14)
        assertThat(side.cross).isLessThanOrEqualTo(side.width)
    }

    @Test
    fun `with room enough beyond the rim, a cell of the preview on the ring at the side is whole`() {
        // Given cells of 42 in a rim of 11: a whole cell passes the rim by 31, which 31 pixels and the margin hold
        val sizes = PadlockSizes(cell = 42, rim = 11)

        // When
        val side = sizes.sideCell(beyond = 35, margin = 4)
        val wider = sizes.sideCell(beyond = 60, margin = 4)

        // Then
        assertThat(listOf(side.width, wider.width)).containsOnly(42)
        assertThat(side.cross).isEqualTo(25)
    }

    @Test
    fun `a cell of the preview on the ring at the side never passes the edge of the screen less the margin`() {
        // Given cells of 42 in a rim of 11, a margin of 4
        val sizes = PadlockSizes(cell = 42, rim = 11)

        // When, from no room at all beyond the rim to much more than a cell
        val passing = (0..60).associateWith { beyond -> sizes.sideCell(beyond, margin = 4).width - sizes.rim }

        // Then what passes the rim stays the margin short of the edge, and the cross within what is shown
        assertThat(passing).allSatisfy { beyond, past -> assertThat(past).isLessThanOrEqualTo(beyond - 4) }
        assertThat((0..60).map { sizes.sideCell(it, margin = 4) }).allSatisfy { side ->
            assertThat(side.cross).isLessThanOrEqualTo(side.width)
        }
    }
}
