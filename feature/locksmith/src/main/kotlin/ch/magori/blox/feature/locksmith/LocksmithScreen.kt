package ch.magori.blox.feature.locksmith

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.board.lock.LockCellCanvas
import ch.magori.blox.core.board.next.NextPieceCanvas
import ch.magori.blox.core.board.next.NextPieceCells
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import kotlin.math.floor

/** What the player does on the screen of the Locksmith, handed to the view model and to `app` by the entry. */
@Immutable
internal class LocksmithActions(
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
        val None = LocksmithActions({}, {}, {}, {}, {}, {}, {}, {})
    }
}

/**
 * The screen of the Locksmith (V35), drawn from [state] alone, always in the Colorful theme, day or night as the
 * system says (decision of the developer, 2026-10-26: the Locksmith has no Original look). From the top: the title and
 * the moves, the chords of the padlock, the padlock, a message, Hint and Restart, the given pieces, then turn left,
 * Place, turn right.
 */
@Composable
internal fun LocksmithScreen(
    state: LocksmithUiState,
    actions: LocksmithActions,
    modifier: Modifier = Modifier,
) {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BloxTheme.palette.page)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(BloxSpaces.space4),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TitleRow(state)
            ChordBand(state)
            PadlockGrid(state, actions.onTap)
            Message(state)
            Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
                SecondaryButton(
                    text = stringResource(R.string.feature_locksmith_hint),
                    icon = BloxIcon.STAR,
                    onClick = actions.onHint,
                    enabled = !state.game.isOpen && state.hint == null,
                )
                SecondaryButton(
                    text = stringResource(R.string.feature_locksmith_restart),
                    icon = BloxIcon.REPLAY,
                    onClick = actions.onRestart,
                )
            }
            Tray(state, actions)
            Controls(state, actions)
        }
    }
}

@Composable
private fun TitleRow(state: LocksmithUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feature_locksmith_title, state.level),
            modifier = Modifier.semantics { heading() },
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.gameTitle,
        )
        Text(
            text = stringResource(R.string.feature_locksmith_moves, state.game.moves),
            color = BloxTheme.palette.label,
            style = BloxTheme.typography.statLabel,
        )
    }
}

/** One pill per chord of the padlock, filled once heard; the last two make the cadence V–I. */
@Composable
private fun ChordBand(state: LocksmithUiState) {
    val heard = state.game.chords.size
    Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
        state.game.progression.numerals.forEachIndexed { index, numeral ->
            val played = index < heard
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
    }
}

/** The padlock, 10 × 10 cells: its ring of metal, then the 8 × 8 inside, each one touched by [onTap]. */
@Composable
private fun PadlockGrid(
    state: LocksmithUiState,
    onTap: (Position) -> Unit,
) {
    val cells = remember(state.game, state.hint, state.aim) { lockCells(state) }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // A whole number of pixels per cell, so that ten of them leave no seam between cells on any density.
        val side = with(LocalDensity.current) { floor(min(MAX_CELL, maxWidth / Padlock.SIZE).toPx()).toDp() }
        Column {
            repeat(Padlock.SIZE) { row ->
                Row {
                    repeat(Padlock.SIZE) { column ->
                        val position = Position(column, row)
                        PadlockCell(cells[position] ?: LockCell.Metal, position, side, onTap)
                    }
                }
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
                    .size(TILE)
                    .then(ring)
                    .semantics {
                        contentDescription = name
                        selected = chosen
                    },
                enabled = !game.isOpen,
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

@Composable
private fun Controls(
    state: LocksmithUiState,
    actions: LocksmithActions,
) {
    val playing = !state.game.isOpen && state.game.selected != null
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
        if (state.game.isOpen) {
            PrimaryButton(
                text = stringResource(R.string.feature_locksmith_next),
                icon = BloxIcon.PLAY,
                onClick = actions.onNext,
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.feature_locksmith_place),
                icon = BloxIcon.PLACE,
                onClick = actions.onPlace,
                enabled = playing && state.aim != null,
            )
        }
        GlyphButton(
            icon = BloxIcon.ROTATE_RIGHT,
            description = stringResource(R.string.feature_locksmith_turn_right),
            onClick = { actions.onTurn(Rotation.CLOCKWISE) },
            enabled = playing,
        )
    }
}

/** The largest cell of the padlock; 10 cells fit the width of a phone of 390 dp, margins included, at 35.8 dp. */
private val MAX_CELL = 42.dp

/** The narrowest pill of a chord. */
private val CHORD_PILL = 44.dp

/** A tile of a given piece, at the size of a touch target. */
private val TILE = 52.dp

/** A cell of a piece in its tile. */
private val TILE_CELL = 10.dp

/**
 * Padlock 3 of seed 7 under play: its first given piece placed where the solution has it, a hint asked, the next piece
 * aimed at the top left corner of the hole, where it does not fit everywhere. Under the Original theme of the
 * application: the screen imposes the Colorful one.
 */
@Composable
private fun LocksmithPreview() {
    val start = PadlockGame.start(Padlock.generate(level = 3, seed = 7))
    val first = start.padlock.solution.first { it.type == start.given.first() }
    val turned = generateSequence(start) { it.turn(Rotation.CLOCKWISE) }.first {
        it.orientationOf(0) ==
            first.orientation
    }
    val placed = turned.place(first.pivot).game
    val hinted = placed.hint()
    val hint = (hinted.events.single() as PadlockEvent.HintGiven).hint
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        LocksmithScreen(
            state = LocksmithUiState(
                level = 3,
                game = hinted.game,
                hint = hint,
                aim = start.padlock.hole.minWith(compareBy(Position::row, Position::column)),
            ),
            actions = LocksmithActions.None,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithDayPreview() {
    LocksmithPreview()
}

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun LocksmithNightPreview() {
    LocksmithPreview()
}

/** The ring of the chosen tile: thicker than the frame of a tile, so that the choice does not rest on colour alone. */
private val SELECTED_RING = 4.dp
