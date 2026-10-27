package ch.magori.blox.core.board.lock

import androidx.compose.ui.unit.Density
import ch.magori.blox.core.board.cellLayout
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.ceil

/** The cross of a refused preview cut at the edge of the screen: as large as its fill holds it with its halo. */
class LockCutCrossTest {
    @Test
    fun `a cell of 42 cut to 23, its cross takes its fill less its halo and 2 dp on each side`() {
        // When: a fill of 21 (a joint of 1 on each side), a stroke of 3 on a halo of 6
        val cross = lockCutCross(side = 42f, width = 23f, density = Density(1f))

        // Then: 21 - 6 - 2 × 2
        assertThat(cross.fill).isEqualTo(21f)
        assertThat(cross.halo).isEqualTo(6f)
        assertThat(cross.side).isEqualTo(11f)
    }

    @ParameterizedTest(name = "a cell of {0} px cut to {1} px at {2} pixels per dp")
    @CsvSource("42, 23, 1", "33, 24, 1", "115, 65, 2.75", "93, 59, 2.75", "42, 42, 1", "115, 100, 2.75")
    fun `at rest, the cross of a cut cell and its halo keep 2 dp on each side within its joints`(
        side: Float,
        width: Float,
        density: Float,
    ) {
        // Given the joint of a cell, 1 dp rounded up to a whole pixel
        val joint = ceil(density)

        // When
        val cross = lockCutCross(side, width, Density(density))

        // Then
        assertThat(cross.reach + 2 * 2 * density).isLessThanOrEqualTo(width - 2 * joint)
    }

    @Test
    fun `a cell cut wide enough keeps the cross of a whole cell, across half its fill`() {
        // When: whole, a fill of 40, and nearly whole
        val whole = lockCutCross(side = 42f, width = 42f, density = Density(1f))
        val nearly = lockCutCross(side = 42f, width = 38f, density = Density(1f))

        // Then: half of 40, as the cross of a cell that is not cut
        assertThat(listOf(whole.side, nearly.side)).containsOnly(20f)
    }

    @Test
    fun `a whole cell of an odd fill keeps the cross of the cells that are not cut, half a pixel included`() {
        // When: a cell of 43, a fill of 41
        val cross = lockCutCross(side = 43f, width = 43f, density = Density(1f))

        // Then: half of 41, as the cross of a cell below the padlock
        assertThat(cross.side).isEqualTo(20.5f)
    }

    @Test
    fun `a whole cell at the side, its cut without a cross of its own, keeps the cross of a cell not cut`() {
        // Given a whole cell of 43 at the side, which a shake may narrow to 37, and the cross of a cell not cut
        val style = lockCellStyle(BloxThemeVariant.COLORFUL_LIGHT, LockCell.Preview(PieceType.T, fits = false))
        val cut = LockCellCut(width = 43, narrowest = 37, cross = null)

        // When
        val side = cutCrossSide(cut, style, side = 43f, density = Density(1f))

        // Then: half of its fill of 41 at rest, whatever is shown
        assertThat(side).isEqualTo(20.5f)
    }

    @Test
    fun `the cross of a cut cell is a whole number of pixels`() {
        // When: a fill of 59 at 2.75 pixels per dp, a halo of 16, 2 dp of 5.5 pixels
        val cross = lockCutCross(side = 115f, width = 65f, density = Density(2.75f))

        // Then: 59 - 16 - 11 = 32
        assertThat(cross.side).isEqualTo(32f)
    }

    @Test
    fun `the cross of a cut cell fits a width as wide as its reach, and no narrower`() {
        // Given a cross of 11 on a halo of 6
        val cross = lockCutCross(side = 42f, width = 23f, density = Density(1f))

        // Then
        assertThat(cross.fitsWithin(17f)).isTrue()
        assertThat(cross.fitsWithin(16f)).isFalse()
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("variantsAndPieces")
    fun `the cross of a cut cell is the same in every Colorful variant and for every piece`(
        variant: BloxThemeVariant,
        type: PieceType,
    ) {
        // Given the style of a refused preview of that piece in that variant
        val style = lockCellStyle(variant, LockCell.Preview(type, fits = false))

        // When
        val cross = lockCutCross(side = 115f, width = 65f, density = Density(2.75f), style = style)

        // Then
        assertThat(cross).isEqualTo(lockCutCross(side = 115f, width = 65f, density = Density(2.75f)))
        assertThat(cellLayout(style, 115f, Density(2.75f), 65f).fill.width).isEqualTo(cross.fill)
    }

    private companion object {
        @JvmStatic
        fun variantsAndPieces(): List<Arguments> =
            listOf(BloxThemeVariant.COLORFUL_LIGHT, BloxThemeVariant.COLORFUL_DARK).flatMap { variant ->
                PieceType.entries.map { arguments(variant, it) }
            }
    }
}
