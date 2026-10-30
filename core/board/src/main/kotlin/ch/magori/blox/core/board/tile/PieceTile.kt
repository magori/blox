package ch.magori.blox.core.board.tile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.next.NextPieceCanvas
import ch.magori.blox.core.board.piece.PieceCells
import ch.magori.blox.core.board.piece.pieceCells
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * A tile of a tray of pieces (the Locksmith, the musical garden): a secondary bubble button, `radius-md`, its [piece]
 * drawn as the next piece of the Classic, 10 dp a cell, centred; without a piece, an empty slot, still touched. It is
 * the size of a touch target, 52 dp, unless the screen gives it another in [modifier], which wins (the wide tiles of
 * the garden, 84 × 52 dp): the piece stays centred, at 10 dp a cell. The [chosen] one bears a ring in `accent`, thicker
 * than its frame so that the choice does not rest on colour alone, which goes down with it when pressed. TalkBack reads
 * it as one button, by its [description] alone, its [stateDescription] when explored (never announced), and whether it
 * is chosen. What a touch does, what it says and its tag belong to the screen ([onClick], [modifier]).
 */
@Composable
fun PieceTile(
    piece: PieceCells?,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chosen: Boolean = false,
    stateDescription: String? = null,
) {
    val ring = if (chosen) Modifier.border(CHOSEN_RING, BloxTheme.palette.accent, BloxTheme.shapes.md) else Modifier
    BubbleButton(
        family = ButtonFamily.SECONDARY,
        onClick = onClick,
        modifier = modifier
            .size(TILE)
            .then(ring)
            .semantics {
                contentDescription = description
                selected = chosen
                if (stateDescription != null) this.stateDescription = stateDescription
            },
        shape = BloxTheme.shapes.md,
    ) {
        if (piece != null) {
            NextPieceCanvas(
                next = piece,
                cellSize = with(LocalDensity.current) { TILE_CELL.toPx() },
                // Its own size whatever the side padding of the button leaves it: none on a tile of 48 dp, where a canvas of
                // no width would draw its cells in black.
                modifier = Modifier
                    .requiredSize(TILE)
                    .clearAndSetSemantics {},
                background = BloxTheme.palette.button,
            )
        }
    }
}

/** A tile, at the size of a touch target. */
private val TILE = 52.dp

/** A cell of a piece in its tile. */
private val TILE_CELL = 10.dp

/** The ring of the chosen tile: thicker than the frame of a tile, so that the choice does not rest on colour alone. */
private val CHOSEN_RING = 4.dp

/** A tray of tiles on the page: the chosen one, two others in their orientation, an empty slot. */
@Preview
@Composable
internal fun PieceTilesPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Row(
            modifier = Modifier
                .background(BloxTheme.palette.page)
                .padding(BloxSpaces.space2),
            horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        ) {
            val right = ClassicRotationSystem.orientations(PieceType.L).first()
            val spawn = ClassicRotationSystem::spawnOrientation
            PieceTile(pieceCells(PieceType.L, right), "Pièce L", onClick = {}, chosen = true)
            PieceTile(pieceCells(PieceType.T, spawn(PieceType.T)), "Pièce T", onClick = {})
            PieceTile(pieceCells(PieceType.I, spawn(PieceType.I)), "Pièce I", onClick = {})
            PieceTile(piece = null, description = "Pièce O, posée", onClick = {})
        }
    }
}

/**
 * The chosen I, the widest piece, in a tile of 52 dp then shrunk to a touch target, 48 dp, as in the tray of the
 * Locksmith on a phone of 360 dp: its cells keep their size, and come up to the ring.
 */
@Preview
@Composable
internal fun PieceTilesShrunkPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Row(
            modifier = Modifier
                .background(BloxTheme.palette.page)
                .padding(BloxSpaces.space2),
            horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        ) {
            val i = pieceCells(PieceType.I, ClassicRotationSystem.spawnOrientation(PieceType.I))
            PieceTile(i, "Pièce I", onClick = {}, chosen = true)
            PieceTile(i, "Pièce I", onClick = {}, modifier = Modifier.size(SHRUNK), chosen = true)
            PieceTile(i, "Pièce I", onClick = {}, modifier = Modifier.size(SHRUNK))
        }
    }
}

/** A tile shrunk to a touch target. */
private val SHRUNK = 48.dp
