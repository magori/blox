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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/** Every board has the same columns and rows: the size of the board does not depend on its cells. */
private val BoardShape = Board.empty()

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
) {
    BoardCanvas(board, piece, painters = ::paintersFor, modifier, variant, background)
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
) {
    // Derived from the board alone: a move of the piece leaves it as it is, and recomposes nothing.
    val speech by remember(board) { derivedStateOf { boardSpeech(board()) } }
    val description = spokenBoard(speech)
    Spacer(
        modifier = modifier
            .semantics { contentDescription = description }
            .drawWithCache {
                val columns = BoardShape.columns
                val geometry = BoardGeometry.fit(size.width, size.height, columns, BoardShape.rows)
                val cellPainters = painters(variant, geometry.cellSize, background)
                onDrawBehind {
                    val cells = displayedCells(board(), piece())
                    for (index in cells.indices) {
                        val left = geometry.origin.x + index % columns * geometry.cellSize
                        val top = geometry.origin.y + index / columns * geometry.cellSize
                        translate(left, top) { cellPainters.getValue(cells[index]).draw(this) }
                    }
                }
            },
    )
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
private fun BoardCanvasColorfulPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        BoardCanvas({ previewBoard }, { previewPiece }, Modifier.size(240.dp, 480.dp))
    }
}
