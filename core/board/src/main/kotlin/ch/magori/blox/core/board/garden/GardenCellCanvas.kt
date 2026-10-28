package ch.magori.blox.core.board.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.CellPainter
import ch.magori.blox.core.board.pixelsOf
import ch.magori.blox.core.board.roundToPixel
import ch.magori.blox.core.designsystem.theme.BloxShapeSets
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.PieceType

/**
 * Draws one [cell] of the musical garden, a square of [size] (`garden-cell`, [GardenSizes.cell], on the screen), in
 * [variant], which must be a Colorful variant: the garden is drawn in the Colorful theme only, and an Original variant
 * fails at once, at composition. Its joint takes the colour under it, the band when it lies [inBand], the board
 * otherwise, so that cells side by side make one grid. Under the [playHead], its part of the column is tinted and
 * framed in ink: a free cell under the tint, a note or a preview over it, its dot and its cross over the frame.
 * No logic here: the look comes from [gardenCellStyle], the drawing from [CellPainter], as the cells of the board and of
 * the Locksmith. The grid, its frame, the bands behind the groups ([gardenBand]) and what TalkBack reads of a cell
 * belong to the screen, which passes the state of each cell.
 * A note plays its [effects], read in the drawing alone (ADR 0024): shown in part or out of its place over the free
 * cell it leaves, larger, lit in the glow of the theme (`clear-glow`), its dot with it.
 */
@Composable
fun GardenCellCanvas(
    cell: GardenCell,
    size: Dp,
    modifier: Modifier = Modifier,
    inBand: Boolean = false,
    playHead: GardenPlayHead? = null,
    effects: GardenCellEffects? = null,
    variant: BloxThemeVariant = BloxTheme.variant,
) {
    require(variant.kind == BloxThemeKind.COLORFUL) {
        "The musical garden is drawn in the Colorful theme only, not in $variant: the screen must impose it."
    }
    val glow = BloxTheme.palette.clearGlow
    Spacer(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val colors = gardenColors(variant)
                val background = if (inBand) colors.band else colors.board
                val side = this.size.height.roundToPixel()
                val cellSize = Size(side, side)
                val painter = CellPainter(
                    style = gardenCellStyle(variant, cell, inBand),
                    side = side,
                    background = background,
                    density = Density(density, fontScale),
                )
                val head = playHead?.let { part ->
                    val look = gardenPlayHeadLook(variant)
                    val stroke = pixelsOf(look.frameWidth, side)
                    val frame = playHeadFrame(part, side)
                    val shape = BloxShapeSets.Colorful.cell
                    // The tint fills the column, the frame runs inside its edge: both cut to the cell.
                    val tint = Path().apply {
                        addOutline(shape.createOutline(Size(side, frame.height), layoutDirection, this@drawWithCache))
                        translate(Offset(0f, frame.top))
                    }
                    val edge = Path().apply {
                        val inner = Size(side - stroke, frame.height - stroke)
                        addOutline(shape.createOutline(inner, layoutDirection, this@drawWithCache))
                        translate(Offset(stroke / 2, frame.top + stroke / 2))
                    }
                    PlayHeadPaths(look, tint, edge, Stroke(stroke))
                }
                // The free cell a note leaves while it is out of its place, or not wholly shown; built once.
                val free = effects?.let {
                    CellPainter(
                        style = gardenCellStyle(variant, GardenCell.Free, inBand),
                        side = side,
                        background = background,
                        density = Density(density, fontScale),
                    )
                }
                val centre = Offset(side / 2, side / 2)
                val bounds = Rect(Offset.Zero, cellSize)
                // The layer of a note not wholly shown, only for a note that plays effects.
                val layer = effects?.let { Paint() }

                // Local functions: compiled to methods, they allocate nothing while drawing.
                fun DrawScope.asTheNote(draw: DrawScope.() -> Unit) {
                    if (effects == null || layer == null) return draw()
                    val shown = effects.shown()
                    if (shown <= 0f) return
                    translate(top = -effects.lift() * side) {
                        scale(effects.scale(), pivot = centre) {
                            if (shown >= 1f) {
                                draw()
                            } else {
                                layer.alpha = shown
                                drawContext.canvas.saveLayer(bounds, layer)
                                draw()
                                drawContext.canvas.restore()
                            }
                        }
                    }
                }

                fun DrawScope.drawFreeUnder() {
                    if (effects == null || free == null) return
                    if (effects.shown() < 1f || effects.lift() != 0f) free.drawBody(this, withJoint = false)
                }

                fun DrawScope.drawBody() = asTheNote {
                    painter.drawBody(this, withJoint = false)
                    val light = effects?.light() ?: 0f
                    if (light > 0f) painter.drawLight(this, glow, light * LIGHT_ALPHA)
                }
                onDrawBehind {
                    drawRect(background, size = cellSize)
                    if (head == null) {
                        drawFreeUnder()
                        drawBody()
                    } else {
                        val (look, tint, edge, stroke) = head
                        clipRect(right = side, bottom = side) {
                            // A free cell lies under the tint; a note or a preview shows over it, as bright as ever.
                            if (cell == GardenCell.Free) painter.drawBody(this, withJoint = false) else drawFreeUnder()
                            drawPath(tint, look.tint)
                            if (cell != GardenCell.Free) drawBody()
                            drawPath(edge, look.frame, style = stroke)
                        }
                    }
                    asTheNote { painter.drawMarks(this) }
                }
            },
    )
}

/** What the play head draws in a cell, built once: its [tint] and the [edge] of its frame, drawn in [stroke]. */
private data class PlayHeadPaths(val look: GardenPlayHeadLook, val tint: Path, val edge: Path, val stroke: Stroke)

/**
 * The extent of the frame of the play head drawn in a cell, from its [top] to its [bottom], in pixels of the cell: it
 * runs on past the edges where the column goes on, so that only the corners at its ends are drawn, once cut to the
 * cell.
 */
internal data class PlayHeadFrame(val top: Float, val bottom: Float) {
    val height: Float get() = bottom - top
}

/** The frame of the play head in a cell of [side] pixels at [part] of the column. */
internal fun playHeadFrame(
    part: GardenPlayHead,
    side: Float,
): PlayHeadFrame = PlayHeadFrame(
    top = if (part == GardenPlayHead.TOP) 0f else -side,
    bottom = if (part == GardenPlayHead.BOTTOM) side else 2 * side,
)

/** The pieces in the order of [PieceType], a note of each in a row. */
private val pieces = PieceType.entries

/** Each kind of cell of the garden, a row per kind: free, the notes, the notes of the chord, the previews, refused. */
private val previewRows: List<List<GardenCell>> = listOf(
    List(pieces.size + 1) { GardenCell.Free },
    pieces.map { GardenCell.Note(it) } + GardenCell.Free,
    pieces.map { GardenCell.Note(it, inChord = true) } + GardenCell.Free,
    pieces.map { GardenCell.Preview(it, fits = true) } + GardenCell.Free,
    pieces.map { GardenCell.Preview(it, fits = false) } + GardenCell.Free,
)

/** The columns of the preview in a band: the second group of 4, as on the screen. */
private const val PREVIEW_BAND_START = 4

/** The column of the preview under the play head. */
private const val PREVIEW_PLAY_HEAD = 2

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render this preview.

/**
 * Every kind of cell of the garden, in cells of 22 dp, the size of the screen, on the board: free cells; the notes of
 * each piece; the same, of the chord; their previews that fit; their previews refused. The last four columns lie in a
 * band, the third under the play head.
 */
@Preview
@Composable
internal fun GardenCellsPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        val variant = BloxTheme.variant
        Column(Modifier.background(gardenColors(variant).board).padding(PREVIEW_RIM)) {
            for ((row, cells) in previewRows.withIndex()) {
                Row {
                    for ((column, cell) in cells.withIndex()) {
                        GardenCellCanvas(
                            cell = cell,
                            size = GardenSizes.cell,
                            inBand = column >= PREVIEW_BAND_START,
                            playHead = if (column == PREVIEW_PLAY_HEAD) previewPlayHead(row) else null,
                        )
                    }
                }
            }
        }
    }
}

/** The part of the column under the play head in [row] of the preview. */
private fun previewPlayHead(row: Int): GardenPlayHead = when (row) {
    0 -> GardenPlayHead.TOP
    previewRows.lastIndex -> GardenPlayHead.BOTTOM
    else -> GardenPlayHead.MIDDLE
}

private val PREVIEW_RIM = 8.dp

/** How bright a note lit is at most: its body shows through, as a cell of the Locksmith lit. */
private const val LIGHT_ALPHA = 0.7f

/** A note under [effects] that never change, as frozen on one frame. */
private class FrozenEffects(
    private val shown: Float = 1f,
    private val lift: Float = 0f,
    private val scale: Float = 1f,
    private val light: Float = 0f,
) : GardenCellEffects {
    override fun shown(): Float = shown

    override fun lift(): Float = lift

    override fun scale(): Float = scale

    override fun light(): Float = light
}

/** The effects of the preview, a column each: at rest, lit, bouncing, falling, half shown. */
private val previewEffects: List<GardenCellEffects> = listOf(
    FrozenEffects(),
    FrozenEffects(light = 1f),
    FrozenEffects(scale = 1.1f),
    FrozenEffects(lift = 0.5f),
    FrozenEffects(shown = 0.5f),
)

/**
 * The effects of a note frozen on one frame, in cells of 22 dp on the board, a column each: at rest, lit, bouncing to
 * 1.1 times its size, half a cell above its place, half shown; a free cell above each; a T of the chord, its dot with
 * it; out of a band, then in a band, then under the play head.
 */
@Preview
@Composable
internal fun GardenCellEffectsPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        val variant = BloxTheme.variant
        Column(Modifier.background(gardenColors(variant).board).padding(PREVIEW_RIM)) {
            for (place in 0..2) {
                for (row in 0..1) {
                    Row {
                        for (effects in previewEffects) {
                            GardenCellCanvas(
                                cell = if (row == 0) GardenCell.Free else GardenCell.Note(PieceType.T, inChord = true),
                                size = GardenSizes.cell,
                                inBand = place == 1,
                                playHead = if (place == 2) previewPlayHead(row) else null,
                                effects = effects.takeIf { row == 1 },
                            )
                        }
                    }
                }
            }
        }
    }
}
