package ch.magori.blox.feature.cartridges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.BoardRowsCanvas
import ch.magori.blox.core.board.pieceColor
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.gloss
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.PieceType

/**
 * The console of the choice of the game (V26), waiting for a cartridge: its slot, its screen of empty cells where an
 * arrow points down into a slot drawn in dashes, its two knobs in the colours of the T and the S, and its eject,
 * unpressable, there being nothing to eject. A decoration: TalkBack reads nothing of it, and it takes no touch.
 */
@Composable
internal fun Console(modifier: Modifier = Modifier) {
    val palette = BloxTheme.palette
    val shapes = BloxTheme.shapes
    Column(
        modifier = modifier
            .clearAndSetSemantics {}
            .widthIn(max = CONSOLE_WIDTH)
            .fillMaxWidth()
            .background(palette.panel, shapes.card)
            .gloss(BloxTheme.glosses.button, shapes.card, BloxBorders.frame)
            .border(BloxBorders.frame, palette.buttonOutline, shapes.card)
            .padding(horizontal = BloxSpaces.space6)
            .padding(top = BloxSpaces.space4, bottom = BloxSpaces.space3),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth(SLOT_WIDTH)
                .height(SLOT_HEIGHT)
                .background(palette.buttonOutline, shapes.pill),
        )
        Screen()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Knob(PieceType.T)
            Spacer(Modifier.size(BloxSpaces.space2))
            Knob(PieceType.S)
            Spacer(Modifier.weight(1f))
            BubbleButton(family = ButtonFamily.ICON, onClick = {}, enabled = false) {
                BloxIcon(BloxIcon.EJECT, size = EJECT_SIZE)
            }
        }
    }
}

/** The screen of the console: rows of empty cells, the arrow down and the dashed slot over them. */
@Composable
private fun Screen() {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.window
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.boardBackground, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(SCREEN_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        BoardRowsCanvas(
            board = { EMPTY_BOARD },
            piece = { null },
            rows = SCREEN_ROWS,
            modifier = Modifier.fillMaxWidth().aspectRatio(SCREEN_RATIO),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BloxIcon(BloxIcon.INSERT, size = INSERT_SIZE, tint = palette.label)
            DashedSlot()
        }
    }
}

/** Where the cartridge would go, drawn in dashes of `label`. */
@Composable
private fun DashedSlot() {
    val color = BloxTheme.palette.label
    val shape = BloxTheme.shapes.cell
    Spacer(
        Modifier
            .size(DASHED_SLOT_WIDTH, DASHED_SLOT_HEIGHT)
            .drawWithCache {
                // Built once per size, density and colour, not at each drawing.
                val dash = DASH.toPx()
                val outline = shape.createOutline(size, layoutDirection, this)
                val stroke = Stroke(
                    width = DASHED_SLOT_STROKE.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
                onDrawBehind { drawOutline(outline, color, style = stroke) }
            },
    )
}

/** A knob of the console, a disc in the colour of the piece [type], framed by `button-outline`. */
@Composable
private fun Knob(type: PieceType) {
    val shape = BloxTheme.shapes.pill
    Box(
        Modifier
            .size(KNOB_SIZE)
            .background(pieceColor(BloxTheme.variant, type), shape)
            .border(BloxBorders.frame, BloxTheme.palette.buttonOutline, shape),
    )
}

private val EMPTY_BOARD = Board.empty()

/** The widest the console stands, as on the mockup. */
private val CONSOLE_WIDTH = 330.dp

/** The slot at the top of the console, in a fraction of its width as on the mockup (240 of 330). */
private const val SLOT_WIDTH = 240f / 330f
private val SLOT_HEIGHT = 14.dp

/** The screen shows the bottom rows of an empty board, as wide as the board. */
private const val SCREEN_ROWS = 5
private const val SCREEN_RATIO = 10f / SCREEN_ROWS
private val SCREEN_PADDING = 6.dp

private val INSERT_SIZE = 40.dp
private val DASHED_SLOT_WIDTH = 70.dp
private val DASHED_SLOT_HEIGHT = 16.dp
private val DASHED_SLOT_STROKE = 2.5.dp
private val DASH = 5.dp

private val KNOB_SIZE = 30.dp

/**
 * The eject in its disc of 48 dp, 22 dp as on the mockup: larger than an icon beside a label, as the pictogram of an
 * icon button stands alone.
 */
private val EJECT_SIZE = 22.dp
