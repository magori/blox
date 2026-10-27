package ch.magori.blox.feature.locksmith

import androidx.compose.ui.unit.Density
import ch.magori.blox.core.board.lock.LockCellSpan
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.ceil
import kotlin.math.roundToInt

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
        val side = sizes.sideCell(beyond = 16, margin = 4, density = ONE)

        // Then the cell keeps 42 dp; the one on the ring is cut 4 dp from the edge: its 11 dp under the rim and 12 more
        assertThat(sizes.cell).isEqualTo(42)
        assertThat(side.width).isEqualTo(23)
        // Its cross, its fill of 21 less its halo of 6 and 2 dp on each side: whole within its joints and 2 dp more
        assertThat(side.cross.side).isEqualTo(11f)
        assertThat(side.cross.fitsWithin(23f - 2 * 1 - 2 * 2)).isTrue()
    }

    @ParameterizedTest(name = "on {0} dp at {1} pixels per dp")
    @CsvSource("390, 1", "320, 1", "390, 2.75", "320, 2.75", "390, 2", "320, 3", "360, 2.75", "320, 3.375")
    fun `the cross of a cell cut at the side keeps 2 dp of its fill on each side of its halo, and 10 dp at least`(
        screen: Int,
        density: Float,
    ) {
        // When
        val padlock = SidesOn(screen, density)
        val side = padlock.side

        // Then: within its joints, 1 dp rounded up, and 2 dp on each side
        assertThat(side.width).isLessThan(padlock.sizes.cell)
        assertThat(side.cross.fitsWithin(side.width - 2 * ceil(density) - 2 * 2 * density)).isTrue()
        assertThat(side.cross.side / density).isGreaterThanOrEqualTo(10f)
    }

    @Test
    fun `on the narrowest screen, 320 dp, the cross of a cell cut at the side is 14 dp`() {
        // When: cells of 33 dp in a rim of 8, 20 dp beyond it, cut to 24 dp: a fill of 22, a halo of 4
        val side = SidesOn(screen = 320, density = 1f).side

        // Then
        assertThat(side.width).isEqualTo(24)
        assertThat(side.cross.side).isEqualTo(14f)
    }

    @Test
    fun `with room enough beyond the rim, a cell of the preview on the ring at the side is whole`() {
        // Given cells of 43 in a rim of 11: a whole cell passes the rim by 32, which 32 pixels and the margin hold
        val sizes = PadlockSizes(cell = 43, rim = 11)

        // When
        val side = sizes.sideCell(beyond = 36, margin = 4, density = ONE)
        val wider = sizes.sideCell(beyond = 60, margin = 4, density = ONE)

        // Then whole, its cut carrying no cross of its own: it keeps that of the cells below, half its fill of 41
        assertThat(listOf(side.width, wider.width)).containsOnly(43)
        assertThat(side.cut(amplitude = 6f).cross).isNull()
        assertThat(side.cross.side).isEqualTo(20.5f)
    }

    @Test
    fun `a cell of the preview on the ring at the side never passes the edge of the screen less the margin`() {
        // Given cells of 42 in a rim of 11, a margin of 4
        val sizes = PadlockSizes(cell = 42, rim = 11)

        // When, from no room at all beyond the rim to much more than a cell
        val passing = (0..60).associateWith { beyond -> sizes.sideCell(beyond, margin = 4, ONE).width - sizes.rim }

        // Then what passes the rim stays the margin short of the edge, once its cross can be 10 dp there (15 beyond the
        // rim: a cut cell of 22, a fill of 20, a halo of 6 and 2 on each side); 3 dp short of it at least before
        assertThat(passing).allSatisfy { beyond, past -> assertThat(past).isLessThanOrEqualTo(beyond - 3) }
        assertThat(passing.filterKeys { it >= 15 }).allSatisfy { beyond, past ->
            assertThat(past).isLessThanOrEqualTo(beyond - 4)
        }
    }

    @ParameterizedTest(name = "on {0} dp at {1} pixels per dp")
    @CsvSource("320, 2.75", "360, 2.75", "320, 3.375")
    fun `where its cross would fall under 10 dp, a cell at the side is cut a little closer to the edge, 3 dp at least`(
        screen: Int,
        density: Float,
    ) {
        // When: 4 dp from the edge, its cross would take 9.5 dp, its stroke kept
        val padlock = SidesOn(screen, density)

        // Then: 3 dp at least, rounded up to a whole pixel, the same at every density
        assertThat(padlock.side.cross.side / density).isGreaterThanOrEqualTo(10f)
        assertThat(padlock.leftGap).isGreaterThanOrEqualTo(ceil(3 * density).toInt())
        assertThat(padlock.leftGap / density).isLessThan(4f)
    }

    @Test
    fun `where even 3 dp from the edge its cross falls under 10 dp, a cell at the side is cut 3 dp from it`() {
        // When: a screen of 280 dp at 2.75 pixels per dp, cells of 80 pixels; 3 dp from the edge, a cross of 9.8 dp
        val padlock = SidesOn(screen = 280, density = 2.75f)

        // Then the closest the cell may come, its cross as large as it then is, its stroke kept
        assertThat(padlock.leftGap).isEqualTo(9)
        assertThat(padlock.side.cross.side / 2.75f).isLessThan(10f)
    }

    @ParameterizedTest(name = "on {0} dp at {1} pixels per dp, shaken by {2} of its amplitude")
    @MethodSource("shakes")
    fun `shownAt keeps the inner edge of a cell at the side where the padlock puts it, at rest or shaken`(
        screen: Int,
        density: Float,
        shake: Float,
    ) {
        // Given
        val side = SidesOn(screen, density).side

        // When
        val shift = shake * SHAKE.value * density
        val left = side.shownAt(shift, onTheLeft = true)
        val right = side.shownAt(shift, onTheLeft = false)

        // Then: the inner edge of the cell at the left is its right end, that of the cell at the right its left end
        assertThat(left.end).isEqualTo(side.width)
        assertThat(right.start).isEqualTo(0)
    }

    @ParameterizedTest(name = "on {0} dp at {1} pixels per dp, shaken by {2} of its amplitude")
    @MethodSource("shakes")
    fun `shownAt trims a cell at the side by whole pixels, its outer edge never closer to the edge than at rest`(
        screen: Int,
        density: Float,
        shake: Float,
    ) {
        // Given
        val side = SidesOn(screen, density).side
        val shift = shake * SHAKE.value * density

        // When
        val left = side.shownAt(shift, onTheLeft = true)
        val right = side.shownAt(shift, onTheLeft = false)

        // Then, relative to where it stands at rest, its outer edge moves with the padlock up to its slack, no further
        assertThat(shift + left.start).isGreaterThanOrEqualTo(-side.slack.toFloat())
        assertThat(shift - (side.width - right.end)).isLessThanOrEqualTo(side.slack.toFloat())
    }

    @Test
    fun `shaken by a fraction of a pixel, a cell at the side is trimmed by the whole pixel above`() {
        // Given a cell of 23 cut 4 from the edge, no slack
        val side = PadlockSizes(cell = 42, rim = 11).sideCell(beyond = 16, margin = 4, density = ONE)

        // Then: 2.3 pixels towards an edge take 3 pixels off that side, nothing off the other
        assertThat(side.shownAt(shift = -2.3f, onTheLeft = true)).isEqualTo(LockCellSpan(start = 3, end = 23))
        assertThat(side.shownAt(shift = 2.3f, onTheLeft = false)).isEqualTo(LockCellSpan(start = 0, end = 20))
        assertThat(side.shownAt(shift = 2.3f, onTheLeft = true)).isEqualTo(LockCellSpan(start = 0, end = 23))
    }

    @ParameterizedTest(name = "on {0} dp at {1} pixels per dp")
    @CsvSource("390, 1", "320, 1", "390, 2.75", "320, 2.75", "360, 2.75", "320, 3.375")
    fun `the narrowest a cell at the side is drawn is reached at the height of the shake, never passed during it`(
        screen: Int,
        density: Float,
    ) {
        // Given
        val side = SidesOn(screen, density).side
        val amplitude = SHAKE.value * density
        val narrowest = side.narrowest(amplitude)

        // When the padlock is shaken by every hundredth of its amplitude, to each side
        val widths = (-100..100).flatMap { step ->
            val shift = amplitude * step / 100
            listOf(true, false).map { onTheLeft -> side.shownAt(shift, onTheLeft).let { it.end - it.start } }
        }

        // Then the painters built ahead, from the narrowest width to the whole one, cover the whole shake
        assertThat(widths.min()).isEqualTo(narrowest)
        assertThat(side.cut(amplitude).narrowest).isEqualTo(narrowest)
    }

    @ParameterizedTest(name = "on {0} dp at {1} pixels per dp")
    @CsvSource("390, 1", "320, 1", "390, 2.75", "320, 2.75", "360, 2.75", "320, 3.375")
    fun `at the peak of the shake, the cross of a side cell and its halo stay within what is shown, joints included`(
        screen: Int,
        density: Float,
    ) {
        // When
        val side = SidesOn(screen, density).side

        // Then, the cross as large as at rest: the 2 dp around its halo are given up for the few frames of the peak
        assertThat(side.cross.fitsWithin(side.narrowest(SHAKE.value * density).toFloat())).isTrue()
    }

    @Test
    fun `at 390 dp, a cell at the side stands 4 dp from the edge of the screen at rest`() {
        assertThat(SidesOn(screen = 390, density = 1f).leftGap).isEqualTo(4)
        assertThat(SidesOn(screen = 390, density = 2.75f).leftGap).isEqualTo(11)
    }

    /** The cells at the side of the padlock on a screen [screen] dp wide at [density], as the screen sizes them. */
    private class SidesOn(screen: Int, density: Float) {
        private val column = (screen - 2 * SIDE_MARGIN.value) * density
        val sizes = padlockSizes(width = column, maxCell = MAX_CELL.value * density)
        private val beyond = sizes.beyond(
            width = column.toInt(),
            sideMargin = (SIDE_MARGIN.value * density).roundToInt(),
        )
        val side = sizes.sideCell(
            beyond,
            margin = (BloxSpaces.space1.value * density).roundToInt(),
            density = Density(density),
        )

        /** Where the outer edge of the cell at the left stands from the edge of the screen at rest, in pixels. */
        val leftGap = beyond - (side.width - sizes.rim)
    }

    private companion object {
        /** One pixel per dp. */
        val ONE = Density(1f)

        /** Screens and densities, the padlock shaken to each side by its amplitude, by fractions of it, or at rest. */
        @JvmStatic
        fun shakes(): List<Arguments> = listOf(
            390 to 1f,
            320 to 1f,
            390 to 2.75f,
            320 to 2.75f,
            360 to 2.75f,
            320 to 3.375f,
        ).flatMap { (screen, density) ->
            listOf(-1f, -0.73f, -0.5f, 0f, 0.37f, 0.5f, 1f).map { arguments(screen, density, it) }
        }
    }
}
