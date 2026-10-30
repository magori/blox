package ch.magori.blox.feature.locksmith

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

/** The tray of the given pieces: tiles of 52 dp at most, 48 dp at least, on one row or two of 4 at most. */
class TrayLayoutTest {
    @Test
    fun `6 tiles of 52 dp hold on one row of a phone of 390 dp`() {
        // When: 390 dp less 16 dp of margin on each side
        val layout = trayLayout(width = 358.dp, count = 6, density = ONE_PIXEL_A_DP)

        // Then
        assertThat(layout).isEqualTo(TrayLayout(tile = 52.dp, perRow = 6))
    }

    @Test
    fun `on a phone of 360 dp, 6 tiles shrink to 48 dp to hold on one row`() {
        // When: 360 dp less the margins, 328 dp, which 6 tiles of 52 dp and their 5 gaps of 8 dp, 352 dp, pass
        val layout = trayLayout(width = 328.dp, count = 6, density = ONE_PIXEL_A_DP)

        // Then: 6 × 48 + 5 × 8 = 328
        assertThat(layout).isEqualTo(TrayLayout(tile = 48.dp, perRow = 6))
    }

    @Test
    fun `rather than shrink under 48 dp, 7 tiles go on two rows, of 4 then 3, at 52 dp`() {
        // When: on one row, (328 - 6 × 8) / 7 = 40 dp
        val layout = trayLayout(width = 328.dp, count = 7, density = ONE_PIXEL_A_DP)

        // Then
        assertThat(layout).isEqualTo(TrayLayout(tile = 52.dp, perRow = 4))
    }

    @Test
    fun `the tiles of a row, laid out in whole pixels, never pass its width`() {
        // Given a column of 889 pixels at 2.625 pixels per dp, where 6 tiles would be 130.67 pixels, rounded up to 131
        val density = Density(2.625f)
        val width = 889

        // When
        val layout = with(density) { trayLayout(width = width.toDp(), count = 6, density = density) }

        // Then
        with(density) {
            val row = 6 * layout.tile.roundToPx() + 5 * BloxSpaces.space2.roundToPx()
            assertThat(row).isLessThanOrEqualTo(width)
        }
    }

    @Test
    fun `where they hold at 52 dp, the tiles are as large as a tile lays itself out, 52 dp rounded to whole pixels`() {
        // Given a phone of 390 dp at 2.625 pixels per dp: 52 dp are 136.5 pixels, laid out as 137
        val density = Density(2.625f)

        // When
        val layout = with(density) { trayLayout(width = 358.dp, count = 5, density = density) }

        // Then
        with(density) { assertThat(layout.tile.roundToPx()).isEqualTo(137) }
    }

    @ParameterizedTest(name = "{0} tiles on {1} dp at {2} pixels a dp")
    @MethodSource("trays")
    fun `from 1 to 8 tiles, on a phone of 320 dp to 412 dp, each is 48 to 52 dp, on two rows of 4 at most`(
        count: Int,
        screen: Int,
        pixelsADp: Float,
    ) {
        // Given the column of the screen, a whole number of pixels, less 16 dp of margin on each side
        val density = Density(pixelsADp)
        val width = with(density) { (screen.dp.roundToPx() - 2 * SIDE_MARGIN.roundToPx()).toDp() }

        // When
        val layout = trayLayout(width = width, count = count, density = density)

        // Then
        val rows = (count + layout.perRow - 1) / layout.perRow
        with(density) { assertThat(layout.tile.roundToPx()).isBetween(48.dp.roundToPx(), 52.dp.roundToPx()) }
        assertThat(rows).isLessThanOrEqualTo(2)
        if (rows == 2) assertThat(layout.perRow).isLessThanOrEqualTo(4)
        with(density) {
            val row = layout.perRow * layout.tile.roundToPx() + (layout.perRow - 1) * BloxSpaces.space2.roundToPx()
            assertThat(row).isLessThanOrEqualTo(width.roundToPx())
        }
    }

    private companion object {
        val ONE_PIXEL_A_DP = Density(1f)

        @JvmStatic
        fun trays(): List<Arguments> = (1..8).flatMap { count ->
            listOf(320, 360, 390, 412).flatMap { screen ->
                listOf(2f, 2.625f, 2.75f, 2.8125f, 3f, 3.5f).map { arguments(count, screen, it) }
            }
        }
    }
}
