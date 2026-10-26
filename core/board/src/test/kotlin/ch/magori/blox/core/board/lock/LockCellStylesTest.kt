package ch.magori.blox.core.board.lock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.CellBubble
import ch.magori.blox.core.board.CellCentre
import ch.magori.blox.core.board.CellCross
import ch.magori.blox.core.board.CellJoint
import ch.magori.blox.core.board.CellStyle
import ch.magori.blox.core.board.cellStyle
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.offset
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class LockCellStylesTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a metal cell is a square of lock metal with a rivet in its centre`(variant: BloxThemeVariant) {
        // Given the colours of the lock
        val colors = lockColors(variant)

        // When
        val style = lockCellStyle(variant, LockCell.Metal)

        // Then a flat square, without joint: metal cells side by side make one plate
        assertThat(style).isEqualTo(
            CellStyle(
                fill = colors.metal,
                border = null,
                centre = CellCentre(colors.rivet, size = 1f / 7),
            ),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a rivet is darker than the metal, so that it does not read as a small hole`(variant: BloxThemeVariant) {
        // Given
        val colors = lockColors(variant)

        // Then the hole is lighter than the metal by day: a lighter rivet would look like one
        assertThat(colors.rivet.luminance()).isLessThan(colors.metal.luminance())
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a hole is an empty cell of the board sunk by the hole inset`(variant: BloxThemeVariant) {
        // When
        val style = lockCellStyle(variant, LockCell.Hole)

        // Then the empty cell of the board, rounded and framed by its joint of 1 px, the metal around it; an inner
        // shadow at the top instead of a flat fill
        assertThat(style).isEqualTo(
            CellStyle(
                fill = palette(variant).emptyCell,
                border = null,
                centre = null,
                joint = CellJoint.Fixed(width = 1.dp),
                bubble = CellBubble(highlight = null, gloss = lockColors(variant).holeInset),
            ),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a placed piece has the bubble it has on the board`(variant: BloxThemeVariant) {
        for (type in PieceType.entries) {
            // When
            val style = lockCellStyle(variant, LockCell.Placed(type))

            // Then its colours, its gloss and its joint, the metal around it
            assertThat(style).`as`("$type").isEqualTo(cellStyle(variant, Cell.Filled(type)))
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a placed piece named by the hint bears the hint mark`(variant: BloxThemeVariant) {
        // Given the mark of a hint on the hole
        val mark = lockCellStyle(variant, LockCell.Hint).dot

        for (type in PieceType.entries) {
            // When the hint names the piece that blocks (Hint.Blocking)
            val style = lockCellStyle(variant, LockCell.Placed(type, hinted = true))

            // Then the placed piece, bearing the same mark
            assertThat(style).`as`("$type").isEqualTo(lockCellStyle(variant, LockCell.Placed(type)).copy(dot = mark))
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a preview that fits differs from its placed piece by more than its colour`(variant: BloxThemeVariant) {
        for (type in PieceType.entries) {
            // Given the piece once placed, in relief, without outline
            val placed = lockCellStyle(variant, LockCell.Placed(type))

            // When
            val style = lockCellStyle(variant, LockCell.Preview(type, fits = true))

            // Then the preview is flat, without the gloss of the cells, and outlined in ink, about 2 dp of a cell of
            // 42 dp, the outline standing out from the empty cell by 3:1 (WCAG 1.4.11)
            assertThat(placed.bubble?.gloss).isNotEqualTo(Gloss.None)
            assertThat(placed.border).isNull()
            assertThat(style.bubble?.gloss).`as`("$type").isEqualTo(Gloss.None)
            val outline = requireNotNull(style.border) { "$type" }
            assertThat(outline.color).isEqualTo(lockColors(variant).ink)
            assertThat(outline.width * 42).isCloseTo(2f, offset(0.5f))
            assertThat(contrast(outline.color, palette(variant).emptyCell)).isGreaterThanOrEqualTo(3.0)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a preview that does not fit is crossed out, so that the refusal does not rest on colour`(
        variant: BloxThemeVariant,
    ) {
        for (type in PieceType.entries) {
            // When
            val style = lockCellStyle(variant, LockCell.Preview(type, fits = false))

            // Then the preview that fits, under a cross in ink on a halo of the page
            val colors = lockColors(variant)
            assertThat(style).`as`("$type").isEqualTo(
                lockCellStyle(variant, LockCell.Preview(type, fits = true)).copy(
                    cross = CellCross(color = colors.ink, halo = colors.page, width = 1f / 14),
                ),
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a hint reads apart from the hole and from every preview without colour`(variant: BloxThemeVariant) {
        // Given the hole and every preview
        val hole = lockCellStyle(variant, LockCell.Hole)
        val previews = PieceType.entries.flatMap { type ->
            listOf(true, false).map { fits -> lockCellStyle(variant, LockCell.Preview(type, fits)) }
        }

        // When
        val style = lockCellStyle(variant, LockCell.Hint)

        // Then a hole, sunk in the metal and tinted as a complement, bearing a mark that neither the hole nor a preview
        // bears: its ink stands out from the hole by 3:1, its ink or its halo from the tinted hint by 3:1
        assertThat(style.bubble?.gloss).isEqualTo(hole.bubble?.gloss)
        assertThat(style.fill).isNotEqualTo(hole.fill)
        val mark = requireNotNull(style.dot)
        assertThat(hole.dot).isNull()
        assertThat(previews).allSatisfy { preview -> assertThat(preview.dot).isNull() }
        assertThat(contrast(mark.color, hole.fill)).isGreaterThanOrEqualTo(3.0)
        assertThat(maxOf(contrast(mark.color, style.fill), contrast(mark.halo, style.fill))).isGreaterThanOrEqualTo(3.0)
    }

    @Test
    fun `the padlock has no style in the Original theme, the Locksmith is drawn in the Colorful theme only`() {
        // Then
        assertThatIllegalArgumentException().isThrownBy { lockCellStyle(BloxThemeVariant.ORIGINAL, LockCell.Metal) }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `the metal stands out from the hole and from the page by 3 to 1 at least`(variant: BloxThemeVariant) {
        // Given
        val metal = lockColors(variant).metal

        // Then the shape of the hole reads (WCAG 1.4.11), and the padlock from the page it lies on, in its rim
        assertThat(contrast(metal, lockCellStyle(variant, LockCell.Hole).fill)).isGreaterThanOrEqualTo(3.0)
        assertThat(contrast(metal, palette(variant).page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `the cross of a refused preview reads on every piece, by its ink or by its halo`(variant: BloxThemeVariant) {
        for (type in PieceType.entries) {
            // Given
            val style = lockCellStyle(variant, LockCell.Preview(type, fits = false))
            val cross = requireNotNull(style.cross)

            // Then the ink stands out from its halo, and the ink or the halo from the bottom and from the top of the
            // preview, its fill and its highlight, by 3:1 (WCAG 1.4.11)
            assertThat(contrast(cross.color, cross.halo)).isGreaterThanOrEqualTo(3.0)
            for (under in listOfNotNull(style.fill, style.bubble?.highlight)) {
                val readable = maxOf(contrast(cross.color, under), contrast(cross.halo, under))
                assertThat(readable).`as`("$type on $under").isGreaterThanOrEqualTo(3.0)
            }
        }
    }

    private fun palette(variant: BloxThemeVariant) = when (variant) {
        BloxThemeVariant.COLORFUL_DARK -> BloxPalettes.ColorfulDark
        else -> BloxPalettes.ColorfulLight
    }
}
