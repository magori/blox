package ch.magori.blox.core.board

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/** Every board has the same columns and rows: the size of the board does not depend on its cells. */
private val BoardShape = Board.empty()

/**
 * Where a [BoardCanvas] of [width] × [height] pixels draws the point at [column] and [row] of its board, in cells from
 * the top-left corner of the board: the cells laid out as it lays them out, in whole pixels, centred. What is drawn over
 * the board, such as a sticker, is placed by it.
 */
fun boardPoint(
    width: Float,
    height: Float,
    column: Float,
    row: Float,
): Offset {
    val geometry = BoardGeometry.fit(width, height, BoardShape.columns, BoardShape.rows)
    return geometry.origin + Offset(column * geometry.cellSize, row * geometry.cellSize)
}

/**
 * Draws the [board] and the [piece] in play in [variant], on [background], the colour of the joints. No
 * logic here: the cells come from [displayedCells], their place from [BoardGeometry], their look from
 * the [CellPainter]s built by [paintersFor].
 *
 * The geometry and the painters, with their paths, are built once per variant, size and background.
 * [board] and [piece] are read only while drawing: a move of the piece draws the board again, without
 * recomposing it nor building anything but its cells. Both must stay the same lambdas from one
 * composition to the next, reading a state rather than capturing a value: a new lambda changes the
 * modifier, and the geometry and the painters are built again.
 *
 * With [effects], each cell is drawn as they say on the frame (ADR 0024), read only while drawing as well: rows lifted
 * above their place, rows that glow in [glow], their cells without joints, cells scaled around their centre or lit in
 * [glow]. The board is clipped to its cells while rows are lifted.
 *
 * TalkBack reads the height of the stack ([boardSpeech]), not the piece in play: a description that
 * followed the piece would change at each move, many times a second, and recompose the board as often.
 * The height changes only when a piece locks or rows are cleared.
 */
@Composable
fun BoardCanvas(
    board: () -> Board,
    piece: () -> Piece?,
    modifier: Modifier = Modifier,
    variant: BloxThemeVariant = BloxTheme.variant,
    background: Color = BloxTheme.palette.boardBackground,
    effects: BoardEffects? = null,
    glow: Color = BloxTheme.palette.clearGlow,
) {
    BoardCanvas(board, piece, painters = ::paintersFor, modifier, variant, background, effects, glow)
}

/** [BoardCanvas] with the [CellPainter]s built by [painters]: the tests count how often they are built. */
@Composable
internal fun BoardCanvas(
    board: () -> Board,
    piece: () -> Piece?,
    painters: PaintersFactory,
    modifier: Modifier = Modifier,
    variant: BloxThemeVariant = BloxTheme.variant,
    background: Color = BloxTheme.palette.boardBackground,
    effects: BoardEffects? = null,
    glow: Color = BloxTheme.palette.clearGlow,
) {
    // Derived from the board alone: a move of the piece leaves it as it is, and recomposes nothing.
    val speech by remember(board) { derivedStateOf { boardSpeech(board()) } }
    val description = spokenBoard(speech)
    BoardDrawing(
        board = board,
        piece = piece,
        rows = BoardShape.rows,
        painters = painters,
        variant = variant,
        background = background,
        modifier = modifier.semantics { contentDescription = description },
        effects = effects,
        glow = glow,
    )
}

/**
 * Draws the bottom [rows] of the [board] and the [piece] in play over them, as [BoardCanvas] does, but decorative:
 * TalkBack reads nothing of it, and nothing recomposes as the board changes. The preview of the home (P5.77), a stack
 * and a piece that falls on it, is drawn so. [board] and [piece] are read only while drawing, as by [BoardCanvas], and
 * must stay the same lambdas from one composition to the next. [rows] goes from 1 to the rows of the board; its size,
 * in cells of a given side, is [boardRowsSize].
 */
@Composable
fun BoardRowsCanvas(
    board: () -> Board,
    piece: () -> Piece?,
    rows: Int,
    modifier: Modifier = Modifier,
    variant: BloxThemeVariant = BloxTheme.variant,
    background: Color = BloxTheme.palette.boardBackground,
) {
    BoardDrawing(board, piece, rows, ::paintersFor, variant, background, modifier)
}

/** The size of the bottom [rows] of the board in cells of [cellSize]: the columns of the board across, a cell per row. */
fun boardRowsSize(
    rows: Int,
    cellSize: Dp,
): DpSize = DpSize(cellSize * BoardShape.columns, cellSize * rows)

/**
 * Draws the bottom [rows] of [board], with [piece] over them, in the cells of [painters], as [effects] say if any
 * ([drawCells]): the geometry and the painters are built once per variant, size and background, the cells read at each
 * drawing. A recomposition, when
 * TalkBack has a new height to read, keeps the drawing as it is, and builds nothing again.
 */
@Composable
private fun BoardDrawing(
    board: () -> Board,
    piece: () -> Piece?,
    rows: Int,
    painters: PaintersFactory,
    variant: BloxThemeVariant,
    background: Color,
    modifier: Modifier = Modifier,
    effects: BoardEffects? = null,
    glow: Color = Color.Unspecified,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val geometry = BoardGeometry.fit(size.width, size.height, BoardShape.columns, rows)
            val cellPainters = painters(variant, geometry.cellSize, background, Density(density, fontScale))
            onDrawBehind {
                drawCells(displayedCells(board(), piece(), rows), rows, geometry, cellPainters, effects ?: AtRest, glow)
            }
        },
    )
}

/**
 * The [cells] of the bottom [rows] of the board, laid out by [geometry], drawn by [painters] as [effects] say (rows counted
 * from the top of those shown: only the whole board has effects), the glow in [glow]: row by row, from the top, a row
 * lifted drawn over the place of the rows above it, which are lifted as much or more, and so never covers them. Where a
 * lifted row stood, empty cells. In a row that glows, the cells that swell are drawn last, over their neighbours.
 * Nothing is allocated per cell. At rest ([AtRest]), each cell is drawn at its place, as it is.
 */
private fun DrawScope.drawCells(
    cells: List<Cell>,
    rows: Int,
    geometry: BoardGeometry,
    painters: Map<Cell, CellPainter>,
    effects: BoardEffects,
    glow: Color,
) {
    val columns = BoardShape.columns
    val side = geometry.cellSize
    val origin = geometry.origin
    val empty = painters.getValue(Cell.Empty)
    val rowSize = Size(columns * side, side)
    val centre = Offset(side / 2, side / 2)

    // A local function: compiled to a method, it allocates nothing.
    fun DrawScope.drawCell(
        column: Int,
        row: Int,
        top: Float,
        glows: Boolean,
    ) {
        val scale = effects.cellScale(column, row)
        if (scale <= 0f) return
        val painter = painters.getValue(cells[row * columns + column])
        val light = effects.cellLight(column, row)
        translate(origin.x + column * side, top) {
            if (scale == 1f) {
                painter.draw(this, withJoint = !glows)
                if (light > 0f) painter.drawLight(this, glow, light)
            } else {
                scale(scale, pivot = centre) {
                    painter.draw(this, withJoint = !glows)
                    if (light > 0f) painter.drawLight(this, glow, light)
                }
            }
        }
    }

    clipRect(origin.x, origin.y, origin.x + rowSize.width, origin.y + rows * side) {
        for (row in 0 until rows) {
            val place = origin.y + row * side
            val lift = effects.rowLift(row) * side
            if (lift != 0f) {
                for (column in 0 until columns) translate(origin.x + column * side, place) { empty.draw(this) }
            }
            val top = place - lift
            val glows = effects.rowGlows(row)
            if (glows) drawRect(glow, topLeft = Offset(origin.x, top), size = rowSize)
            for (column in 0 until columns) {
                if (effects.cellScale(column, row) <= 1f) drawCell(column, row, top, glows)
            }
            // The cells that swell, over their neighbours.
            for (column in 0 until columns) {
                if (effects.cellScale(column, row) > 1f) drawCell(column, row, top, glows)
            }
        }
    }
}

/** What TalkBack reads of the board, from [speech]. */
@Composable
@ReadOnlyComposable
private fun spokenBoard(speech: BoardSpeech): String = when (speech) {
    BoardSpeech.Empty -> stringResource(R.string.core_board_empty)

    is BoardSpeech.Stack -> pluralStringResource(
        R.plurals.core_board_stack,
        speech.height,
        speech.height,
        speech.rows,
    )
}

/** A board holding the seven pieces, and a T in play as it spawns, for the previews. */
private val previewBoard = Board.parse(
    """
    J.........
    JJJ..OO..L
    SSTTTOO.LL
    IIII.ZZSSL
    """,
)

private val previewPiece = Piece(
    PieceType.T,
    ClassicRotationSystem.spawnOrientation(PieceType.T),
    pivot = Position(column = 4, row = 6),
)

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview
@Composable
internal fun BoardCanvasOriginalPreview() {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        BoardCanvas({ previewBoard }, { previewPiece }, Modifier.size(240.dp, 480.dp))
    }
}

@Preview
@Composable
internal fun BoardCanvasColorfulPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        BoardCanvas({ previewBoard }, { previewPiece }, Modifier.size(240.dp, 480.dp))
    }
}

/** Cells of 12 dp, under 16 dp: corners of 2 dp, no gloss. */
@Preview
@Composable
internal fun BoardCanvasColorfulSmallPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        BoardCanvas({ previewBoard }, { previewPiece }, Modifier.size(120.dp, 240.dp))
    }
}

/** No effect: the board drawn as it is, by the same drawing as with effects. */
private val AtRest = object : BoardEffects {}
