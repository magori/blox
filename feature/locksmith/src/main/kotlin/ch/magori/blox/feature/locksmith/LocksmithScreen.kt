package ch.magori.blox.feature.locksmith

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.board.lock.LockCellCanvas
import ch.magori.blox.core.board.lock.LockCellCut
import ch.magori.blox.core.board.lock.LockCellLight
import ch.magori.blox.core.board.lock.LockCellShown
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
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.PieceType
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
 * turn right. Once the padlock is open, the card « Serrure ouverte ! » takes the place of the buttons, the tiles and the
 * controls, and the focus, once; the message stays, and takes the focus back when the padlock starts over from the
 * card. The feedback of the last action plays on the padlock ([Feedback]), from its tick 0 on the frames of the screen,
 * or frozen at [feedbackTick] for the previews; the feedback of the action shown first is not played again.
 */
@Composable
internal fun LocksmithScreen(
    state: LocksmithUiState,
    actions: LocksmithActions,
    modifier: Modifier = Modifier,
    feedbackTick: Int? = null,
) {
    // The effects of the application kept: the theme imposed here would bring back the full ones.
    BloxTheme(kind = BloxThemeKind.COLORFUL, effects = BloxTheme.effects) {
        val card = openCardOf(state)
        val titleFocus = remember { FocusRequester() }
        val messageFocus = remember { FocusRequester() }
        OpeningFocus(state.game.isOpen, titleFocus, messageFocus)
        val feedback = remember(state) { feedbackOf(state) }
        val tick = rememberFeedbackTick(feedback, state.move, feedbackTick)
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
                        .padding(horizontal = SIDE_MARGIN, vertical = BloxSpaces.space3),
                    verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TopBar(state, actions.onBack)
                    ChordBand(state)
                    PadlockGrid(state, actions.onTap, feedback, tick)
                    Message(state, messageFocus)
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
                    if (card == null) Controls(state, actions) else OpenPadlockCard(card, actions, titleFocus)
                }
            }
        }
    }
}

/**
 * Gives the focus where [focusAfter] says once the padlock opens or starts over from the card: to the title of the
 * card, [titleFocus], or to the message, [messageFocus]. Whether it [isOpen] is kept across a recreation, so that
 * coming back to the screen moves nothing.
 */
@Composable
private fun OpeningFocus(
    isOpen: Boolean,
    titleFocus: FocusRequester,
    messageFocus: FocusRequester,
) {
    var wasOpen by rememberSaveable { mutableStateOf(isOpen) }
    LaunchedEffect(isOpen) {
        when (focusAfter(wasOpen, isOpen)) {
            FocusTarget.CARD_TITLE -> titleFocus.requestFocus()
            FocusTarget.MESSAGE -> messageFocus.requestFocus()
            null -> Unit
        }
        wasOpen = isOpen
    }
}

/**
 * The tick of [feedback] the padlock shows: counted on the frames of the screen from its start, until it is over in
 * the effects of the theme; [frozen] at a tick for the previews. The feedback of the action [move] shown first, as on
 * coming back to the screen, is over already.
 */
@Composable
private fun rememberFeedbackTick(
    feedback: Feedback?,
    move: Int,
    frozen: Int?,
): () -> Int {
    val first = remember { move }
    // Set as soon as the feedback changes, so that its first frame never shows the tick of the one before.
    val ticks = remember(feedback, frozen) {
        val start = when {
            frozen != null -> frozen
            feedback == null || feedback.move == first -> Int.MAX_VALUE
            else -> 0
        }
        mutableIntStateOf(start)
    }
    val effects = BloxTheme.effects
    LaunchedEffect(ticks, effects) {
        if (frozen != null) return@LaunchedEffect
        val length = feedback?.ticks(effects) ?: return@LaunchedEffect
        val frames = FrameTicks()
        while (ticks.intValue < length) {
            withFrameNanos { ticks.intValue += frames.advance(it) }
        }
    }
    return remember(ticks) { { ticks.intValue } }
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
        val title = stringResource(barTitleOf(state), state.level)
        Text(
            text = stringResource(R.string.feature_locksmith_title, state.level),
            modifier = Modifier
                .weight(1f)
                // Its text kept, TalkBack reads the description in its place.
                .semantics {
                    contentDescription = title
                    heading()
                },
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.gameTitle,
        )
        Text(
            text = stringResource(R.string.feature_locksmith_moves, state.game.moves),
            color = BloxTheme.palette.label,
            style = BloxTheme.typography.statLabel,
        )
        PadlockStatus(open = state.game.isOpen)
    }
}

/**
 * A padlock of [STATUS_ICON], closed or [open], in ink; neither touched nor read: the title of the bar tells its state,
 * and the card alone says that the padlock is open.
 */
@Composable
private fun PadlockStatus(open: Boolean) {
    val ink = BloxTheme.palette.ink
    val page = BloxTheme.palette.page
    Canvas(modifier = Modifier.size(STATUS_ICON)) {
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
    val reading = chordBandReading(chordsOf(state))
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = reading },
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
    ) {
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

/** What TalkBack reads of the band of [chords]: those heard, then those to come, each by its degree and its quality. */
@Composable
@ReadOnlyComposable
private fun chordBandReading(chords: ChordsRead): String {
    val heard = chordNames(chords.heard)
    val coming = chordNames(chords.coming)
    return when (val text = chordBandOf(chords)) {
        R.string.feature_locksmith_chords_none_heard -> stringResource(text, coming)
        R.string.feature_locksmith_chords_all_heard -> stringResource(text, heard)
        else -> stringResource(text, heard, coming)
    }
}

/** How TalkBack reads [numerals], one after the other: « 5 majeur, 1 majeur ». */
@Composable
@ReadOnlyComposable
private fun chordNames(numerals: List<RomanNumeral>): String {
    val separator = stringResource(R.string.feature_locksmith_chords_separator)
    return numerals.map { stringResource(chordNameOf(it), it.degree.toString()) }.joinToString(separator)
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
 * padlock; a collection of 8 rows and 8 columns for TalkBack. A preview whose cells lie on the ring shows them over the
 * rim and beyond it, crossed out ([rimCells]), neither touched nor read: the rim is too thin to hold a cross. Above and
 * below, the room of such a cell is kept ([PadlockSizes.overflow]): it covers neither the chords nor the message, and
 * the padlock does not move with the aim. At the sides, such a cell is cut [BloxSpaces.space1] from the edge of the
 * screen when it would pass it ([PadlockSizes.sideCell]), [SIDE_MARGIN] being left between the column and that edge;
 * shaken with the padlock, it narrows rather than come closer to that edge ([SideCell.shownAt]). The [feedback] of the
 * last action lights its cells and shakes the padlock, at the [tick] read in the drawing alone.
 */
@Composable
private fun PadlockGrid(
    state: LocksmithUiState,
    onTap: (Position) -> Unit,
    feedback: Feedback?,
    tick: () -> Int,
) {
    val cells = remember(state) { lockCells(state) }
    val rim = remember(state) { rimCells(state) }
    val hinted = hintedPieceOf(state)
    val effects = BloxTheme.effects
    val glow = if (feedback?.kind == FeedbackKind.REFUSED) BloxTheme.palette.danger else BloxTheme.palette.clearGlow
    // Over its neighbours, which the preview on the ring may cover.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().zIndex(1f), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val width = with(density) { maxWidth.toPx() }
        val sizes = with(density) { padlockSizes(width = width, maxCell = MAX_CELL.toPx()) }
        val side = with(density) { sizes.cell.toDp() }
        val thickness = with(density) { sizes.rim.toDp() }
        val room = with(density) { sizes.room(gap = BloxSpaces.space3.roundToPx(), open = state.game.isOpen).toDp() }
        val amplitude = with(density) { SHAKE.toPx() }
        val sideCell = remember(sizes, width, density) {
            with(density) {
                val beyond = sizes.beyond(width = width.toInt(), sideMargin = SIDE_MARGIN.roundToPx())
                sizes.sideCell(beyond = beyond, margin = BloxSpaces.space1.roundToPx(), density = this)
            }
        }
        val cut = remember(sideCell, amplitude) { sideCell.cut(amplitude) }
        // Read in the drawing alone: the light of a cell and the shake, frame by frame, recompose nothing.
        val glows = remember(feedback, tick, effects) {
            CellGlows { cell -> feedback?.glow(cell, tick(), effects) ?: 0f }
        }
        val shift = remember(feedback, tick, effects, amplitude) {
            PadlockShift { (feedback?.shake(tick(), effects) ?: 0f) * amplitude }
        }
        Box(modifier = Modifier.padding(vertical = room)) {
            Box(
                modifier = Modifier
                    .graphicsLayer { translationX = shift.pixels() }
                    .background(lockMetal(BloxTheme.variant), RoundedCornerShape(thickness))
                    .padding(thickness),
            ) {
                Column(
                    modifier = Modifier.semantics {
                        collectionInfo = CollectionInfo(rowCount = INSIDE.count(), columnCount = INSIDE.count())
                    },
                ) {
                    for (row in INSIDE) {
                        Row {
                            for (column in INSIDE) {
                                val position = Position(column, row)
                                val cell = cells[position] ?: LockCell.Metal
                                PadlockCell(cell, position, side, hinted, onTap, glows, glow)
                            }
                        }
                    }
                }
                rim.forEach { (position, cell) ->
                    if (position.column in INSIDE) {
                        RimCell(cell, position, side, glows, glow)
                    } else {
                        SideRimCell(cell, position, side, sideCell, cut, shift, glows, glow)
                    }
                }
            }
        }
    }
}

/** How far the padlock is shaken aside, in pixels, to the right when positive, read in the drawing alone. */
private fun interface PadlockShift {
    fun pixels(): Float
}

/** How bright each cell of the padlock is lit, from 0 to 1, read in the drawing alone. */
private fun interface CellGlows {
    fun at(cell: Position): Float
}

/** The light of the cell at [position], in [color], as [glows] say, read in the drawing alone. */
@Composable
private fun lightOf(
    glows: CellGlows,
    position: Position,
    color: Color,
): LockCellLight = remember(glows, position, color) { LockCellLight({ glows.at(position) }, color) }

/**
 * A cell of the preview on the ring above or below the padlock, [cell] at [position], a whole cell of [side] beyond the
 * rim. Neither touched nor read. Lit in [glow] as [glows] say.
 */
@Composable
private fun RimCell(
    cell: LockCell,
    position: Position,
    side: Dp,
    glows: CellGlows,
    glow: Color,
) {
    LockCellCanvas(
        cell = cell,
        size = side,
        modifier = Modifier
            .offset(x = side * (position.column - INSIDE.first), y = side * (position.row - INSIDE.first))
            .clearAndSetSemantics { testTag = rimCellTestTag(position) },
        light = lightOf(glows, position, glow),
    )
}

/**
 * A cell of the preview on the ring at the side of the padlock, [cell] at [position], in column 0 or 9: [cut] as
 * [sideCell] says, as much of it shown as the edge of the screen leaves while the padlock is shaken by [shift]; a cell
 * on the left keeps its inner part, against the rim. Whole, its cut carries no cross of its own, and it keeps that of a
 * whole cell. Neither touched nor read. Lit in [glow] as [glows] say.
 */
@Composable
private fun SideRimCell(
    cell: LockCell,
    position: Position,
    side: Dp,
    sideCell: SideCell,
    cut: LockCellCut,
    shift: PadlockShift,
    glows: CellGlows,
    glow: Color,
) {
    val onTheLeft = position.column < INSIDE.first
    val shown = remember(sideCell, shift, onTheLeft) {
        LockCellShown { sideCell.shownAt(shift.pixels(), onTheLeft) }
    }
    val left = if (onTheLeft) with(LocalDensity.current) { -cut.width.toDp() } else side * INSIDE.count()
    LockCellCanvas(
        cell = cell,
        size = side,
        modifier = Modifier
            .offset(x = left, y = side * (position.row - INSIDE.first))
            .clearAndSetSemantics { testTag = rimCellTestTag(position) },
        cut = cut,
        shown = shown,
        light = lightOf(glows, position, glow),
    )
}

/** A cell inside the ring, [cell] at [position], touched by [onTap]; lit in [glow] as [glows] say. */
@Composable
private fun PadlockCell(
    cell: LockCell,
    position: Position,
    side: Dp,
    hinted: PieceType?,
    onTap: (Position) -> Unit,
    glows: CellGlows,
    glow: Color,
) {
    val description = cellDescription(cell, position, hinted)
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
            .semantics {
                contentDescription = description
                collectionItemInfo = CollectionItemInfo(
                    rowIndex = position.row - INSIDE.first,
                    rowSpan = 1,
                    columnIndex = position.column - INSIDE.first,
                    columnSpan = 1,
                )
            },
        light = lightOf(glows, position, glow),
    )
}

/** What TalkBack reads of [cell], at [position]: a cell marked by the hint names the [hinted] piece. */
@Composable
@ReadOnlyComposable
private fun cellDescription(
    cell: LockCell,
    position: Position,
    hinted: PieceType?,
): String {
    val what = when (cell) {
        LockCell.Metal -> stringResource(R.string.feature_locksmith_cell_metal)

        LockCell.Hole -> stringResource(R.string.feature_locksmith_cell_hole)

        LockCell.Hint -> stringResource(R.string.feature_locksmith_cell_hint, hinted?.name.orEmpty())

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

/**
 * What the last action told, the piece named by the hint in it. While the padlock is closed, TalkBack reads it without
 * interrupting, a refusal with its number, so that the same refusal twice in a row is told twice; once it is open, the
 * card tells it, and the message is no longer announced. It takes the focus from [focus] when the padlock starts over
 * from the card.
 */
@Composable
private fun Message(
    state: LocksmithUiState,
    focus: FocusRequester,
) {
    val text = messageText(state)
    val open = state.game.isOpen
    val spoken = when {
        open -> stringResource(R.string.feature_locksmith_message_cadence_spoken, chordNames(cadenceOf(state)))
        else -> refusalNumberOf(state)?.let { stringResource(R.string.feature_locksmith_message_refusal, it, text) }
    }
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .focusable()
            .semantics {
                if (!open) liveRegion = LiveRegionMode.Polite
                if (spoken != null) contentDescription = spoken
            },
        color = BloxTheme.palette.label,
        style = BloxTheme.typography.small,
        textAlign = TextAlign.Center,
    )
}

/**
 * The text of the message of [state], with what it names: the piece just turned and its orientation, or the piece named
 * by the hint.
 */
@Composable
@ReadOnlyComposable
private fun messageText(state: LocksmithUiState): String {
    val message = messageOf(state)
    val rank = state.game.selected
    return if (message == R.string.feature_locksmith_message_turned && rank != null) {
        stringResource(message, state.game.given[rank].name, stringResource(orientationOf(state.game, rank)))
    } else {
        stringResource(message, hintedPieceOf(state)?.name.orEmpty())
    }
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

/**
 * The given pieces, one tile each, in their orientation, read with it; a placed one is an empty slot, touched to take it
 * back. The chosen one bears a ring, which goes down with it when pressed.
 */
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
            val orientation = if (placed) null else stringResource(orientationOf(game, rank))
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
                        // Read on exploring it; a turn is told by the message, the one region announced.
                        if (orientation != null) stateDescription = orientation
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
 * The card « Serrure ouverte ! »: its title, the one text that tells the opening, a heading that takes the focus from
 * [titleFocus]; the counters of [card], then Serrure suivante, the action of the screen, and Rejouer cette serrure. The
 * cadence that closed the phrase is told by the message, above it.
 */
@Composable
private fun OpenPadlockCard(
    card: OpenCard,
    actions: LocksmithActions,
    titleFocus: FocusRequester,
) {
    WindowPanel(
        title = stringResource(R.string.feature_locksmith_open_title),
        // The title the card had before the windows took screen-title (P5.98): its design is the Serrurier's own.
        titleStyle = BloxTheme.typography.title.merge(MaterialTheme.typography.titleSmall),
        titleModifier = Modifier
            .focusRequester(titleFocus)
            .focusable(),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Counter(stringResource(R.string.feature_locksmith_open_moves), card.moves)
            Counter(stringResource(R.string.feature_locksmith_open_refusals), card.refusals)
            Counter(stringResource(R.string.feature_locksmith_open_hints), card.hints)
        }
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
internal val MAX_CELL = 42.dp

/** The narrowest pill of a chord. */
private val CHORD_PILL = 44.dp

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

/** How far a refusal shakes the padlock aside, at most. */
internal val SHAKE = 6.dp

/** The margin of the column of the screen on each side, which the padlock counts to cut a cell at the edge. */
internal val SIDE_MARGIN = BloxSpaces.space4
