package ch.magori.blox.feature.locksmith

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.board.lock.LockCellCanvas
import ch.magori.blox.core.board.lock.lockMetal
import ch.magori.blox.core.board.next.NextPieceCanvas
import ch.magori.blox.core.board.next.NextPieceCells
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.component.WindowPanel
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.music.RomanNumeral

/** What the player does on the screen of the Locksmith, handed to the view model and to `app` by the entry. */
@Immutable
internal class LocksmithActions(
    val onBack: () -> Unit,
    val onTap: (Position) -> Unit,
    val onSelect: (rank: Int) -> Unit,
    val onTakeBack: (rank: Int) -> Unit,
    val onTurn: (Rotation) -> Unit,
    val onPlace: () -> Unit,
    val onHint: () -> Unit,
    val onRestart: () -> Unit,
    val onNext: () -> Unit,
) {
    companion object {
        /** Actions that do nothing, for the previews. */
        val None = LocksmithActions(
            onBack = {},
            onTap = {},
            onSelect = {},
            onTakeBack = {},
            onTurn = {},
            onPlace = {},
            onHint = {},
            onRestart = {},
            onNext = {},
        )
    }
}

/**
 * The screen of the Locksmith (V35), drawn from [state] alone, always in the Colorful theme, day or night as the
 * system says (decision of the developer, 2026-10-26: the Locksmith has no Original look). From the top, scrolling on
 * a small screen: the bar (back, title, moves, the padlock of its state), the chords of the padlock, the padlock, a
 * message, Hint and Restart, the given pieces. Anchored at the bottom, within reach of the thumb: turn left, Place,
 * turn right. Once the padlock is open, the card « Serrure ouverte » takes the place of the buttons, the tiles and the
 * controls; the message stays.
 */
@Composable
internal fun LocksmithScreen(
    state: LocksmithUiState,
    actions: LocksmithActions,
    modifier: Modifier = Modifier,
) {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        val card = openCardOf(state)
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(BloxTheme.palette.page)
                .safeDrawingPadding(),
        ) {
            val bottom = maxHeight * BOTTOM_SHARE
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = BloxSpaces.space4, vertical = BloxSpaces.space3),
                    verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TopBar(state, actions.onBack)
                    ChordBand(state)
                    PadlockGrid(state, actions.onTap)
                    Message(state)
                    if (card == null) {
                        HintAndRestart(state, actions)
                        Tray(state, actions)
                    }
                }
                // At most a share of the height: the card, scrolling within, never pushes the bar and the padlock off.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = bottom)
                        .padding(BloxSpaces.space4),
                    contentAlignment = Alignment.Center,
                ) {
                    if (card == null) Controls(state, actions) else OpenPadlockCard(card, actions)
                }
            }
        }
    }
}

/** Back, the title and the moves, then the padlock of the state of the game, closed, then open. */
@Composable
private fun TopBar(
    state: LocksmithUiState,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            description = stringResource(R.string.feature_locksmith_back),
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        Text(
            text = stringResource(R.string.feature_locksmith_title, state.level),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.gameTitle,
        )
        Text(
            text = stringResource(R.string.feature_locksmith_moves, state.game.moves),
            color = BloxTheme.palette.label,
            style = BloxTheme.typography.statLabel,
        )
        PadlockStatus(open = state.game.isOpen, description = stringResource(padlockStatusOf(state)))
    }
}

/** A padlock of [STATUS_ICON], closed or [open], in ink; not touched, TalkBack reads [description]. */
@Composable
private fun PadlockStatus(
    open: Boolean,
    description: String,
) {
    val ink = BloxTheme.palette.ink
    val page = BloxTheme.palette.page
    Canvas(
        modifier = Modifier
            .size(STATUS_ICON)
            .semantics {
                contentDescription = description
                role = Role.Image
            },
    ) {
        drawPadlock(ink, page, open)
    }
}

/**
 * A padlock in [ink]: its body, with a keyhole of the [page], and its shackle, down in the body, or lifted out of it on
 * the right when [open].
 */
private fun DrawScope.drawPadlock(
    ink: Color,
    page: Color,
    open: Boolean,
) {
    val w = size.width
    val h = size.height
    val lift = if (open) h * SHACKLE_LIFT else 0f
    val shackle = Path().apply {
        moveTo(w * 0.3f, h * 0.46f)
        lineTo(w * 0.3f, h * 0.3f - lift)
        cubicTo(w * 0.3f, h * 0.02f - lift, w * 0.7f, h * 0.02f - lift, w * 0.7f, h * 0.3f - lift)
        lineTo(w * 0.7f, if (open) h * 0.3f - lift else h * 0.46f)
    }
    drawPath(shackle, ink, style = Stroke(width = w * 0.12f))
    drawRoundRect(
        color = ink,
        topLeft = Offset(w * 0.14f, h * 0.44f),
        size = Size(w * 0.72f, h * 0.5f),
        cornerRadius = CornerRadius(w * 0.08f),
    )
    drawCircle(page, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.66f))
}

/**
 * One pill per chord of the padlock, filled once heard; the last two, V and I, joined under them by a bracket and the
 * word « cadence »: the pair that closes the phrase.
 */
@Composable
private fun ChordBand(state: LocksmithUiState) {
    val heard = state.game.chords.size
    val numerals = state.game.progression.numerals
    val cadence = numerals.size - CADENCE
    Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
        numerals.take(cadence).forEachIndexed { index, numeral -> ChordPill(numeral, played = index < heard) }
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
                numerals.drop(cadence).forEachIndexed { index, numeral ->
                    ChordPill(numeral, played = cadence + index < heard)
                }
            }
            CadenceBracket(BloxTheme.palette.label)
            Text(
                text = stringResource(R.string.feature_locksmith_cadence),
                color = BloxTheme.palette.label,
                style = BloxTheme.typography.statLabel,
            )
        }
    }
}

@Composable
private fun ChordPill(
    numeral: RomanNumeral,
    played: Boolean,
) {
    val fill = if (played) BloxTheme.palette.accent else BloxTheme.palette.page
    val frame = if (played) BloxTheme.palette.accent else BloxTheme.palette.buttonOutline
    Box(
        modifier = Modifier
            .widthIn(min = CHORD_PILL)
            .background(fill, BloxTheme.shapes.pill)
            .border(BloxBorders.frame, frame, BloxTheme.shapes.pill)
            .padding(horizontal = BloxSpaces.space2, vertical = BloxSpaces.space1),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = numeral.name,
            color = if (played) BloxTheme.palette.accentInk else BloxTheme.palette.ink,
            style = BloxTheme.typography.buttonLabel,
        )
    }
}

/** A bracket opening upwards, as wide as the pills above it, in [color]. */
@Composable
private fun CadenceBracket(color: Color) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BloxSpaces.space2, vertical = BloxSpaces.space1)
            .height(BRACKET)
            .drawBehind {
                val stroke = BloxBorders.frame.toPx()
                val bottom = size.height - stroke / 2
                val right = size.width - stroke / 2
                drawLine(color, Offset(0f, bottom), Offset(size.width, bottom), stroke)
                drawLine(color, Offset(stroke / 2, 0f), Offset(stroke / 2, bottom), stroke)
                drawLine(color, Offset(right, 0f), Offset(right, bottom), stroke)
            },
    )
}

/**
 * The padlock: its [INSIDE] × [INSIDE] cells, each one touched by [onTap], in a thin rim of metal, the ring of the
 * padlock. A preview whose cells lie on the ring shows them over the rim and beyond it, crossed out ([rimCells]),
 * neither touched nor read: the rim is too thin to hold a cross. Above and below, the room of such a cell is kept
 * ([PadlockSizes.overflow]): it covers neither the chords nor the message, and the padlock does not move with the aim.
 */
@Composable
private fun PadlockGrid(
    state: LocksmithUiState,
    onTap: (Position) -> Unit,
) {
    val cells = remember(state.game, state.hint, state.aim) { lockCells(state) }
    val rim = remember(state.game, state.aim) { rimCells(state) }
    // Over its neighbours, which the preview on the ring may cover.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().zIndex(1f), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val sizes = with(density) { padlockSizes(width = maxWidth.toPx(), maxCell = MAX_CELL.toPx()) }
        val side = with(density) { sizes.cell.toDp() }
        val thickness = with(density) { sizes.rim.toDp() }
        val room = with(density) { sizes.room(gap = BloxSpaces.space3.roundToPx(), open = state.game.isOpen).toDp() }
        Box(
            modifier = Modifier
                .padding(vertical = room)
                .background(lockMetal(BloxTheme.variant), RoundedCornerShape(thickness))
                .padding(thickness),
        ) {
            Column {
                for (row in INSIDE) {
                    Row {
                        for (column in INSIDE) {
                            val position = Position(column, row)
                            PadlockCell(cells[position] ?: LockCell.Metal, position, side, onTap)
                        }
                    }
                }
            }
            rim.forEach { (position, cell) ->
                LockCellCanvas(
                    cell = cell,
                    size = side,
                    modifier = Modifier
                        .offset(x = side * (position.column - INSIDE.first), y = side * (position.row - INSIDE.first))
                        .clearAndSetSemantics {},
                )
            }
        }
    }
}

@Composable
private fun PadlockCell(
    cell: LockCell,
    position: Position,
    side: Dp,
    onTap: (Position) -> Unit,
) {
    val description = cellDescription(cell, position)
    val action = stringResource(
        when (cell) {
            is LockCell.Preview -> R.string.feature_locksmith_action_place
            is LockCell.Placed -> R.string.feature_locksmith_action_take_back
            else -> R.string.feature_locksmith_action_aim
        },
    )
    LockCellCanvas(
        cell = cell,
        size = side,
        modifier = Modifier
            .testTag(cellTestTag(cell))
            .clickable(role = Role.Button, onClickLabel = action) { onTap(position) }
            .semantics { contentDescription = description },
    )
}

@Composable
private fun cellDescription(
    cell: LockCell,
    position: Position,
): String {
    val what = when (cell) {
        LockCell.Metal -> stringResource(R.string.feature_locksmith_cell_metal)

        LockCell.Hole -> stringResource(R.string.feature_locksmith_cell_hole)

        LockCell.Hint -> stringResource(R.string.feature_locksmith_cell_hint)

        is LockCell.Placed -> stringResource(
            if (cell.hinted) R.string.feature_locksmith_cell_placed_hinted else R.string.feature_locksmith_cell_placed,
            cell.type.name,
        )

        is LockCell.Preview -> stringResource(
            if (cell.fits) R.string.feature_locksmith_cell_preview else R.string.feature_locksmith_cell_preview_refused,
            cell.type.name,
        )
    }
    return stringResource(R.string.feature_locksmith_cell, position.column, position.row, what)
}

/** What the last action told, read by TalkBack without interrupting. */
@Composable
private fun Message(state: LocksmithUiState) {
    Text(
        text = stringResource(messageOf(state)),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = BloxTheme.palette.label,
        style = BloxTheme.typography.small,
        textAlign = TextAlign.Center,
    )
}

/** Hint, greyed while a hint is shown, and Restart. */
@Composable
private fun HintAndRestart(
    state: LocksmithUiState,
    actions: LocksmithActions,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
        SecondaryButton(
            text = stringResource(R.string.feature_locksmith_hint),
            icon = BloxIcon.STAR,
            onClick = actions.onHint,
            enabled = state.hint == null,
        )
        SecondaryButton(
            text = stringResource(R.string.feature_locksmith_restart),
            icon = BloxIcon.REPLAY,
            onClick = actions.onRestart,
        )
    }
}

/** The given pieces, one tile each, in their orientation; a placed one is an empty slot, touched to take it back. */
@Composable
private fun Tray(
    state: LocksmithUiState,
    actions: LocksmithActions,
) {
    val game = state.game
    Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
        game.given.forEachIndexed { rank, type ->
            val placed = rank in game.placed
            val chosen = rank == game.selected
            val name = stringResource(
                if (placed) R.string.feature_locksmith_tile_placed else R.string.feature_locksmith_tile,
                type.name,
            )
            val ring = if (chosen) {
                Modifier.border(SELECTED_RING, BloxTheme.palette.accent, BloxTheme.shapes.md)
            } else {
                Modifier
            }
            BubbleButton(
                family = ButtonFamily.SECONDARY,
                onClick = { if (placed) actions.onTakeBack(rank) else actions.onSelect(rank) },
                modifier = Modifier
                    .testTag(tileTestTag(rank))
                    .size(TILE)
                    .then(ring)
                    .semantics {
                        contentDescription = name
                        selected = chosen
                    },
                shape = BloxTheme.shapes.md,
            ) {
                if (!placed) TilePiece(state, rank)
            }
        }
    }
}

/**
 * The given piece of [rank], in its orientation, brought to the top left corner, drawn as the next piece of the
 * Classic, centred in its tile; the tile itself tells TalkBack its name.
 */
@Composable
private fun TilePiece(
    state: LocksmithUiState,
    rank: Int,
) {
    val cells = state.game.orientationOf(rank).broughtToOrigin().cells
    val piece = NextPieceCells(
        type = state.game.given[rank],
        cells = cells,
        columns = cells.maxOf { it.column } + 1,
        rows = cells.maxOf { it.row } + 1,
    )
    NextPieceCanvas(
        next = piece,
        cellSize = with(LocalDensity.current) { TILE_CELL.toPx() },
        modifier = Modifier
            .size(TILE)
            .clearAndSetSemantics {},
        background = BloxTheme.palette.button,
    )
}

/** Turn left, Place, turn right: anchored at the bottom of the screen, within reach of the thumb. */
@Composable
private fun Controls(
    state: LocksmithUiState,
    actions: LocksmithActions,
) {
    val playing = state.game.selected != null
    Row(
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            icon = BloxIcon.ROTATE_LEFT,
            description = stringResource(R.string.feature_locksmith_turn_left),
            onClick = { actions.onTurn(Rotation.COUNTERCLOCKWISE) },
            enabled = playing,
        )
        PrimaryButton(
            text = stringResource(R.string.feature_locksmith_place),
            icon = BloxIcon.PLACE,
            onClick = actions.onPlace,
            modifier = Modifier.testTag(PLACE_TAG),
            enabled = playing && state.aim != null,
        )
        GlyphButton(
            icon = BloxIcon.ROTATE_RIGHT,
            description = stringResource(R.string.feature_locksmith_turn_right),
            onClick = { actions.onTurn(Rotation.CLOCKWISE) },
            enabled = playing,
        )
    }
}

/**
 * The card « Serrure ouverte »: the counters of [card], the cadence that closed the phrase, then Serrure suivante, the
 * action of the screen, and Rejouer cette serrure.
 */
@Composable
private fun OpenPadlockCard(
    card: OpenCard,
    actions: LocksmithActions,
) {
    WindowPanel(title = stringResource(R.string.feature_locksmith_open_title)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Counter(stringResource(R.string.feature_locksmith_open_moves), card.moves)
            Counter(stringResource(R.string.feature_locksmith_open_refusals), card.refusals)
            Counter(stringResource(R.string.feature_locksmith_open_hints), card.hints)
        }
        Text(
            text = stringResource(R.string.feature_locksmith_open_cadence),
            modifier = Modifier.fillMaxWidth(),
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.small,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.feature_locksmith_next),
            icon = BloxIcon.PLAY,
            onClick = actions.onNext,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.feature_locksmith_replay),
            icon = BloxIcon.REPLAY,
            onClick = actions.onRestart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A counter of the card: its [value] over its [label], read by TalkBack as one « label : value ». */
@Composable
private fun Counter(
    label: String,
    value: Int,
) {
    val read = stringResource(R.string.feature_locksmith_open_counter, label, value)
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = read },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value.toString(), color = BloxTheme.palette.ink, style = BloxTheme.typography.statValue)
        Text(text = label, color = BloxTheme.palette.label, style = BloxTheme.typography.statLabel)
    }
}

/** The share of the height the bottom of the screen, the controls or the card, takes at most. */
private const val BOTTOM_SHARE = 0.5f

/** The largest cell of the padlock; 8 cells and their rim fit the width of a phone of 390 dp, margins included. */
private val MAX_CELL = 42.dp

/** The narrowest pill of a chord. */
private val CHORD_PILL = 44.dp

/** The chords of the cadence V–I, the last two of a padlock. */
private const val CADENCE = 2

/** The height of the bracket under the cadence. */
private val BRACKET = 6.dp

/** The padlock of the state of the game, in the bar. */
private val STATUS_ICON = 24.dp

/** How far the shackle of an open padlock rises out of its body, in parts of its height. */
private const val SHACKLE_LIFT = 0.14f

/** A tile of a given piece, at the size of a touch target. */
private val TILE = 52.dp

/** A cell of a piece in its tile. */
private val TILE_CELL = 10.dp

/** The ring of the chosen tile: thicker than the frame of a tile, so that the choice does not rest on colour alone. */
private val SELECTED_RING = 4.dp
