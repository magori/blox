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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.CellPainter
import ch.magori.blox.core.board.roundToPixel
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.PieceType

/**
 * Draws one [cell] of a padlock, a square of [size], in [variant], which must be a Colorful variant: the Locksmith is
 * drawn in the Colorful theme only, and an Original variant fails at once, at composition. Its joint takes the colour
 * of the metal, so that cells side by side make one padlock.
 * No logic here: the look comes from [lockCellStyle], the drawing from [CellPainter], as the cells of the board. The
 * grid, its rim and what TalkBack reads of it belong to the screen. A [cut] cell, at the edge of the screen, is as high,
 * narrower, its cross recentred in what is left of it.
 */
@Composable
fun LockCellCanvas(
    cell: LockCell,
    size: Dp,
    modifier: Modifier = Modifier,
    variant: BloxThemeVariant = BloxTheme.variant,
    cut: LockCellCut? = null,
) {
    require(variant.kind == BloxThemeKind.COLORFUL) {
        "The padlock of the Locksmith is drawn in the Colorful theme only, not in $variant: the screen must impose it."
    }
    Spacer(
        modifier = modifier
            .size(width = cut?.width ?: size, height = size)
            .drawWithCache {
                val style = lockCellStyle(variant, cell)
                val side = this.size.height.roundToPixel()
                val painter = CellPainter(
                    style = style,
                    side = side,
                    background = lockColors(variant).metal,
                    density = Density(density, fontScale),
                    width = this.size.width.roundToPixel(),
                    crossSide = cut?.cross?.toPx(),
                )
                onDrawBehind { painter.draw(this) }
            },
    )
}

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
