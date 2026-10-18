package ch.magori.blox.feature.game.sidepanel

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.board.BoardGeometry
import ch.magori.blox.feature.game.board.CellPainter
import ch.magori.blox.feature.game.board.cellStyle

/**
 * Draws the next piece of [type] in [theme], with the cells of the board, of [cellSize] pixels, on
 * [background], the colour of their joints (the panel of the next piece), centred in the space given
 * to it; nothing when [type] is null. No logic here: the cells come from [nextPieceCells], their place
 * from [BoardGeometry], their look from [CellPainter].
 */
@Composable
internal fun NextPieceCanvas(
    type: PieceType?,
    rotationSystem: RotationSystem,
    cellSize: Float,
    modifier: Modifier = Modifier,
    theme: BloxThemeKind = BloxTheme.kind,
    background: Color = BloxTheme.palette.panel,
) {
    val next = remember(type, rotationSystem) { type?.let { nextPieceCells(it, rotationSystem) } }
    val description = type?.let { pieceName(it) } ?: stringResource(R.string.feature_game_next_none)
    Spacer(
        modifier = modifier
            .semantics { contentDescription = description }
            .drawWithCache {
                if (type == null || next == null) return@drawWithCache onDrawBehind {}
                val geometry = BoardGeometry.centred(size.width, size.height, next.columns, next.rows, cellSize)
                val painter = CellPainter(cellStyle(theme, Cell.Filled(type)), geometry.cellSize, background)
                onDrawBehind {
                    for (cell in next.cells) {
                        val topLeft = geometry.cellTopLeft(cell)
                        translate(topLeft.x, topLeft.y) { painter.draw(this) }
                    }
                }
            },
    )
}

/** The name of a piece, for TalkBack. */
@Composable
@ReadOnlyComposable
internal fun pieceName(type: PieceType): String = stringResource(
    when (type) {
        PieceType.I -> R.string.feature_game_piece_i
        PieceType.O -> R.string.feature_game_piece_o
        PieceType.T -> R.string.feature_game_piece_t
        PieceType.S -> R.string.feature_game_piece_s
        PieceType.Z -> R.string.feature_game_piece_z
        PieceType.J -> R.string.feature_game_piece_j
        PieceType.L -> R.string.feature_game_piece_l
    },
)
