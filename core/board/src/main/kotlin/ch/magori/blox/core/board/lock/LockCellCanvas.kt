package ch.magori.blox.core.board.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.CellPainter
import ch.magori.blox.core.board.CellStyle
import ch.magori.blox.core.board.cellLayout
import ch.magori.blox.core.board.roundToPixel
import ch.magori.blox.core.board.wholeCross
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.PieceType

/**
 * Draws one [cell] of a padlock, a square of [size], in [variant], which must be a Colorful variant: the Locksmith is
 * drawn in the Colorful theme only, and an Original variant fails at once, at composition. Its joint takes the colour
 * of the metal, so that cells side by side make one padlock.
 * No logic here: the look comes from [lockCellStyle], the drawing from [CellPainter], as the cells of the board. The
 * grid, its rim and what TalkBack reads of it belong to the screen. A [cut] cell, at the side of the screen, is as high,
 * as wide as the screen leaves it, and as much of it as is [shown], read in the drawing alone, frame by frame; its cross
 * keeps its size, recentred in what is shown. Its painters, one for each width it may be shown at, are built once.
 * Lit by its [light], read in the drawing alone: the light passes over its body and under its marks, the dot of a hint
 * and the cross of a refusal staying in ink.
 */
@Composable
fun LockCellCanvas(
    cell: LockCell,
    size: Dp,
    modifier: Modifier = Modifier,
    variant: BloxThemeVariant = BloxTheme.variant,
    cut: LockCellCut? = null,
    shown: LockCellShown? = null,
    light: LockCellLight? = null,
) {
    require(variant.kind == BloxThemeKind.COLORFUL) {
        "The padlock of the Locksmith is drawn in the Colorful theme only, not in $variant: the screen must impose it."
    }
    val width = cut?.let { with(LocalDensity.current) { it.width.toDp() } } ?: size
    Spacer(
        modifier = modifier
            .size(width = width, height = size)
            .drawWithCache {
                val style = lockCellStyle(variant, cell)
                val side = this.size.height.roundToPixel()
                val whole = this.size.width.roundToPixel().toInt()
                val cellDensity = Density(density, fontScale)
                val crossSide = cut?.let { cutCrossSide(it, style, side, cellDensity) }
                val painterOf = { shownWidth: Int ->
                    CellPainter(
                        style = style,
                        side = side,
                        background = lockColors(variant).metal,
                        density = cellDensity,
                        width = shownWidth.toFloat(),
                        crossSide = crossSide,
                    )
                }
                val atRest = painterOf(whole)
                // One painter for each width a shake may show, built here, never while drawing.
                val narrowest = cut?.narrowest?.coerceAtMost(whole) ?: whole
                val painters = shown?.let { Array(whole - narrowest + 1) { painterOf(narrowest + it) } }
                val corner = CornerRadius(side * LIGHT_CORNER)
                onDrawBehind {
                    val span = shown?.span()
                    val start = span?.start?.coerceIn(0, whole) ?: 0
                    val end = span?.end?.coerceIn(start, whole) ?: whole
                    val painter = painters?.getOrNull(end - start - narrowest) ?: atRest
                    translate(left = start.toFloat()) {
                        painter.drawBody(this)
                        if (light != null) {
                            val glow = light.glow.brightness()
                            if (glow > 0f) {
                                drawRoundRect(
                                    color = light.color,
                                    size = Size((end - start).toFloat(), side),
                                    cornerRadius = corner,
                                    alpha = glow * LIGHT_ALPHA,
                                )
                            }
                        }
                        painter.drawMarks(this)
                    }
                }
            },
    )
}

/**
 * The side of the cross of a [cut] cell of [side] pixels in [style]: that of its [LockCellCut.cross], or, a whole cell,
 * that of the cells that are not cut, across half its fill at rest; the same whatever a shake shows of it.
 */
internal fun cutCrossSide(
    cut: LockCellCut,
    style: CellStyle,
    side: Float,
    density: Density,
): Float = cut.cross?.side ?: wholeCross(cellLayout(style, side, density, cut.width.toFloat()).fill.width)

/** How bright a cell lit by the screen is at most: its body shows through. */
private const val LIGHT_ALPHA = 0.7f

/** The corners of the light of a cell, in parts of the cell. */
private const val LIGHT_CORNER = 0.18f

/** Each kind of cell of a padlock, a row per kind, the pieces in the order of [PieceType]. */
private val previewRows: List<List<LockCell>> = listOf(
    listOf(LockCell.Metal, LockCell.Hole, LockCell.Hint, LockCell.Metal),
    PieceType.entries.map { LockCell.Placed(it) },
    PieceType.entries.map { LockCell.Placed(it, hinted = true) },
    PieceType.entries.map { LockCell.Preview(it, fits = true) },
    PieceType.entries.map { LockCell.Preview(it, fits = false) },
)

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render this preview.

/**
 * Every kind of cell of a padlock, in cells of 42 dp, the largest of the screen, on its metal: metal, hole, hint; the
 * pieces placed; the same, named by a hint; their previews that fit; their previews refused.
 */
@Preview
@Composable
internal fun LockCellsPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Column(Modifier.background(lockColors(BloxTheme.variant).metal).padding(PREVIEW_RIM)) {
            for (row in previewRows) {
                Row {
                    for (cell in row) LockCellCanvas(cell, PREVIEW_CELL)
                }
            }
        }
    }
}

private val PREVIEW_CELL = 42.dp
private val PREVIEW_RIM = 8.dp
