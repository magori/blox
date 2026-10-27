package ch.magori.blox.feature.composer.grid

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.R
import ch.magori.blox.feature.composer.model.CELLS_PER_BAR
import ch.magori.blox.feature.composer.model.Track
import kotlin.math.min

/** The sizes of the grid, in one place. */
private object GridSizes {
    /** The column of the names of the lines. */
    val labelWidth = 44.dp

    /** Inside the frame of the grid, around its lines, in Colorful (`space-2`). */
    val blockPadding = 8.dp

    /** The fade over the bottom of the grid while lines remain below. */
    val fade = 24.dp

    /** The space that sets an octave apart from the octave above. */
    val octaveSpace = 6.dp

    /**
     * The lowest line: its cells stay 24 dp high at least (WCAG 2.2, 2.5.8) once the gap under them is taken, and the
     * sixteen lines of a music opened with a note out of the scale fit a phone of 760 dp. The
     * lines that do not fit then scroll, on a small phone or with a large font.
     */
    val minRowHeight = 26.dp

    /** Between two cells, and the more between two beats. */
    val cellGap = 1.dp
    val beatGap = 3.dp

    /** Around the attack of a note, inside its cell. */
    val noteInset = 2.dp

    /** The outline of the watermark, so that it shows on a note of the open track too. */
    val echoOutline = 1.5.dp

    /** The edges of the held part of a note, above and below its band, in the colour of its track. */
    val heldEdge = 2.dp

    /** The outline of the column played, over the cells: its line, and the halo inside it. */
    val playedLine = 2.dp
    val playedHalo = 1.dp
}

/** The held part of a note: a band across the middle of the cell, of this part of its height. */
private const val BAND_HEIGHT = 0.4f

/** The watermark of a note of the other track: a dot of this part of the smaller side of the cell, in radius. */
private const val ECHO_RADIUS = 0.14f

/** The lines of the grid, from the highest down: a class of its own, so that the grid is skipped when they are equal. */
@Immutable
internal data class GridLines(val rows: List<RowView>)

/**
 * The grid of one bar of the open [track] (V8, V11): the line « Temps 1 2 3 4 », then its [lines], as high as the room
 * allows but [GridSizes.minRowHeight] at least, scrolling when they do not fit. Touching a cell calls [onTouch] with
 * its line and its cell, counted from 0; touching a cell and sliding along the line calls [onSlide] once the finger is
 * lifted. While the open track is [empty], a hint says how to place a note. Each line is skipped when it did not
 * change: a touch recomposes the lines it changed only. While the music is listened to, an outline surrounds the
 * column played, the [playedEighth] cell of the bar, if it is the bar shown; drawn over the lines, it recomposes none.
 *
 * In Colorful (V33), the grid is a block of `board` framed in `panel-border`, « TEMPS 1 2 3 4 » in capitals, and its
 * bottom fades while lines remain below; each note is a bubble ([noteLook]), its painter built once per look and size
 * ([NotePainters]), never at a touch. Original keeps its flat grid.
 */
@Composable
internal fun NoteGrid(
    lines: GridLines,
    track: Track,
    empty: Boolean,
    playedEighth: Int?,
    onTouch: (pitch: Pitch, eighth: Int) -> Unit,
    onSlide: (pitch: Pitch, fromEighth: Int, toEighth: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val variant = BloxTheme.variant
    // Original is frozen (P5.86): its grid stays that of V8, flat, unframed.
    val dressed = variant != BloxThemeVariant.ORIGINAL
    val colors = cellColors(palette, track)
    val note = noteLook(variant, palette, BloxTheme.glosses, track)
    val outline = playedOutline(palette)
    val density = LocalDensity.current
    // Once per density, not at each touch: each painter is built on its first note of a look and a size.
    val painters = remember(density) { NotePainters { look, size -> NotePainter(look, size, density) } }
    val scroll = rememberScrollState()
    val fade = remember(scroll, palette.boardBackground) { Modifier.fadeBelow(scroll, palette.boardBackground) }
    Column(modifier = modifier) {
        val block = if (dressed) Modifier.gridBlock(palette, BloxTheme.shapes.md) else Modifier
        Column(modifier = Modifier.weight(1f).fillMaxWidth().then(block)) {
            BeatLine(capitals = dressed)
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().then(if (dressed) fade else Modifier)) {
                val rowHeight = rowHeight(lines, if (constraints.hasBoundedHeight) maxHeight else null)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .drawWithContent {
                            drawContent()
                            if (playedEighth != null) drawPlayedColumn(playedEighth, outline)
                        },
                ) {
                    lines.rows.forEach { row ->
                        GridLine(
                            row = row,
                            colors = colors,
                            note = note,
                            painters = painters,
                            outline = outline,
                            onTouch = onTouch,
                            onSlide = onSlide,
                            modifier = Modifier.height(rowHeight + if (row.spaceAbove) GridSizes.octaveSpace else 0.dp),
                        )
                    }
                }
            }
        }
        EmptyHint(shown = empty)
    }
}

/**
 * The block of the grid in Colorful (V33): the `board` of the game, framed by 2 dp of `panel-border`, corners
 * `radius-md`, the lines inside it.
 */
private fun Modifier.gridBlock(
    palette: BloxPalette,
    shape: Shape,
): Modifier = background(palette.boardBackground, shape)
    .border(BloxBorders.frame, palette.panelBorder, shape)
    .padding(GridSizes.blockPadding)

/**
 * A fade to [background] over the bottom of the grid while lines remain below, out of sight, under [scroll]: the grid
 * says it goes on. Read while drawing, the scroll redraws the fade, and recomposes nothing; its gradient is built once
 * per size.
 */
private fun Modifier.fadeBelow(
    scroll: ScrollState,
    background: Color,
): Modifier = drawWithCache {
    val height = min(GridSizes.fade.toPx(), size.height)
    val top = size.height - height
    val brush = Brush.verticalGradient(listOf(Color.Transparent, background), startY = top, endY = size.height)
    val area = Size(size.width, height)
    onDrawWithContent {
        drawContent()
        if (scroll.canScrollForward) drawRect(brush, topLeft = Offset(0f, top), size = area)
    }
}

/** The height of each line of [lines] in [room], the octave spaces left out: [GridSizes.minRowHeight] at least. */
private fun rowHeight(
    lines: GridLines,
    room: Dp?,
): Dp {
    if (room == null) return GridSizes.minRowHeight
    val spaces = GridSizes.octaveSpace * lines.rows.count { it.spaceAbove }
    return max((room - spaces) / lines.rows.size, GridSizes.minRowHeight)
}

/**
 * « Temps 1 2 3 4 », each beat above its two cells, in `stat-label`: in [capitals], as the labels of the panels of the
 * game, in Colorful (V33).
 */
@Composable
private fun BeatLine(capitals: Boolean) {
    val palette = BloxTheme.palette
    val style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.statLabel)
    val beats = stringResource(R.string.feature_composer_beats)
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        SingleLineText(
            text = if (capitals) beats.uppercase() else beats,
            color = palette.label,
            style = style,
            modifier = Modifier.width(GridSizes.labelWidth),
        )
        (1..BEATS).forEach { beat ->
            Text(
                text = beat.toString(),
                color = palette.label,
                style = style,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A line of the grid: its name, then its eight cells, the gaps between them in the line of the grid. The name of a
 * line out of the scale is dimmer, and in italics: its colour is not its only cue. While a finger slides along the
 * line, the cells it crosses are framed with the [outline] of the column played; read in the draw phase, the frame
 * recomposes nothing.
 */
@Composable
private fun GridLine(
    row: RowView,
    colors: CellColors,
    note: NoteLook,
    painters: NotePainters<NotePainter>,
    outline: PlayedOutline,
    onTouch: (pitch: Pitch, eighth: Int) -> Unit,
    onSlide: (pitch: Pitch, fromEighth: Int, toEighth: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val spoken = spokenName(row)
    val touch by rememberUpdatedState(onTouch)
    val slide by rememberUpdatedState(onSlide)
    var crossed by remember { mutableStateOf<IntRange?>(null) }
    Row(
        modifier = modifier.padding(top = if (row.spaceAbove) GridSizes.octaveSpace else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SingleLineText(
            text = shownName(row.name),
            color = if (row.inScale) palette.label else palette.outOfScale,
            style = MaterialTheme.typography.labelSmall,
            fontStyle = if (row.inScale) null else FontStyle.Italic,
            modifier = Modifier.width(GridSizes.labelWidth).clearAndSetSemantics { contentDescription = spoken },
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(colors.line)
                .pointerInput(row.pitch) {
                    detectCellGestures(
                        onTouch = { eighth -> touch(row.pitch, eighth) },
                        onSlide = { from, to -> slide(row.pitch, from, to) },
                        onCrossed = { crossed = it },
                    )
                }
                .drawWithContent {
                    drawContent()
                    crossed?.let { drawCrossed(it, outline) }
                },
        ) {
            row.cells.forEachIndexed { eighth, view ->
                val echo = row.otherTrack[eighth]
                val name = cellName(spoken, eighth)
                val state = cellState(view, echo)
                val action = actionLabel(row.actions[eighth])
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = gapBefore(eighth), bottom = GridSizes.cellGap)
                        .drawBehind { drawCell(view, echo, colors, note, painters) }
                        .testTag("composer_cell")
                        .semantics {
                            contentDescription = name
                            stateDescription = state
                            role = Role.Button
                            onClick(label = action) {
                                touch(row.pitch, eighth)
                                true
                            }
                        },
                )
            }
        }
    }
}

/**
 * The hint of an empty grid, under the grid, so that it hides no line. Its place stays when it is not [shown], hidden
 * and silent: placing the first note does not move the grid.
 */
@Composable
private fun EmptyHint(
    shown: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleLineText(
        text = stringResource(R.string.feature_composer_empty_grid),
        color = BloxTheme.palette.ink,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .alpha(if (shown) 1f else 0f)
            .then(if (shown) Modifier else Modifier.clearAndSetSemantics {}),
    )
}

/** The space before the [eighth] cell of a bar: wider before a beat, none before the first cell. */
private fun gapBefore(eighth: Int) = when {
    eighth == 0 -> 0.dp
    eighth % CELLS_PER_BEAT == 0 -> GridSizes.beatGap
    else -> GridSizes.cellGap
}

/**
 * Taps and slides on the cells of a line, each cell an eighth of its width, as [CellGestureTracker] tells them apart.
 * An event already consumed gives the gesture up: a cancelled gesture (the system back gesture) ends with one, and so
 * does a vertical drag the scroll of the grid took. [onCrossed] gets the cells the slide crosses as the finger moves,
 * and `null` once the gesture ends, however it ends.
 */
private suspend fun PointerInputScope.detectCellGestures(
    onTouch: (eighth: Int) -> Unit,
    onSlide: (fromEighth: Int, toEighth: Int) -> Unit,
    onCrossed: (IntRange?) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown()
    val gesture = CellGestureTracker(
        cellWidth = size.width / CELLS_PER_BAR.toFloat(),
        touchSlop = viewConfiguration.touchSlop,
        downX = down.position.x,
        downY = down.position.y,
    )
    try {
        followCellGesture(down.id, gesture, onTouch, onSlide, onCrossed)
    } finally {
        onCrossed(null)
    }
}

/** Follows the finger [pointer] of [gesture] until it is lifted or the gesture is given up. */
private suspend fun AwaitPointerEventScope.followCellGesture(
    pointer: PointerId,
    gesture: CellGestureTracker,
    onTouch: (eighth: Int) -> Unit,
    onSlide: (fromEighth: Int, toEighth: Int) -> Unit,
    onCrossed: (IntRange?) -> Unit,
) {
    while (!gesture.isAbandoned) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer } ?: return
        if (change.isConsumed) gesture.cancel() else gesture.move(change.position.x, change.position.y)
        onCrossed(gesture.crossed)
        if (change.changedToUp()) {
            when (val done = gesture.lift()) {
                is CellGesture.Touch -> onTouch(done.eighth)
                is CellGesture.Slide -> onSlide(done.fromEighth, done.toEighth)
                null -> Unit
            }
            return
        }
        // Once the finger slides along the line, the slide is its own: the scroll of the grid no longer takes it.
        if (gesture.isSliding) change.consume()
    }
}

/**
 * The outline of the column of the [eighth] cell, over the lines: its line, then its halo just inside it. Each cell
 * takes an eighth of the width left by the names of the lines, the gap before it included, as the cells of [GridLine]
 * do.
 */
private fun DrawScope.drawPlayedColumn(
    eighth: Int,
    outline: PlayedOutline,
) {
    val label = GridSizes.labelWidth.toPx()
    val slot = (size.width - label) / CELLS_PER_BAR
    val gap = gapBefore(eighth).toPx()
    drawOutline(Rect(Offset(label + eighth * slot + gap, 0f), Size(slot - gap, size.height)), outline)
}

/**
 * The outline of the [cells] a slide crosses, on the cells of one line: the line and the halo of the column played.
 * Each cell takes an eighth of the width, the gap before it included, as in [GridLine].
 */
private fun DrawScope.drawCrossed(
    cells: IntRange,
    outline: PlayedOutline,
) {
    val slot = size.width / CELLS_PER_BAR
    val left = cells.first * slot + gapBefore(cells.first).toPx()
    val width = (cells.last + 1) * slot - left
    drawOutline(Rect(Offset(left, 0f), Size(width, size.height - GridSizes.cellGap.toPx())), outline)
}

/** [outline] around [bounds]: its line, then its halo just inside it. */
private fun DrawScope.drawOutline(
    bounds: Rect,
    outline: PlayedOutline,
) {
    val line = GridSizes.playedLine.toPx()
    val halo = GridSizes.playedHalo.toPx()
    drawFrame(bounds, inset = line / 2, width = line, color = outline.line)
    drawFrame(bounds, inset = line + halo / 2, width = halo, color = outline.halo)
}

/** A frame of [width] in [color], its middle [inset] inside [bounds]. */
private fun DrawScope.drawFrame(
    bounds: Rect,
    inset: Float,
    width: Float,
    color: Color,
) {
    drawRect(
        color = color,
        topLeft = Offset(bounds.left + inset, bounds.top + inset),
        size = Size(bounds.width - 2 * inset, bounds.height - 2 * inset),
        style = Stroke(width = width),
    )
}

/**
 * A cell: its background, its note, whole or as two sixteenth notes, then the watermark of the other track, outlined
 * in the background so that it shows on a note too.
 */
private fun DrawScope.drawCell(
    view: CellView,
    echo: Boolean,
    colors: CellColors,
    note: NoteLook,
    painters: NotePainters<NotePainter>,
) {
    drawRect(colors.background)
    if (view.isSplit) {
        drawPart(view.first, left = 0f, width = size.width / 2, colors, note, painters)
        drawPart(view.second, left = size.width / 2, width = size.width / 2, colors, note, painters)
    } else {
        drawPart(view.first, left = 0f, width = size.width, colors, note, painters)
    }
    if (echo) {
        val radius = min(size.width, size.height) * ECHO_RADIUS
        drawCircle(colors.echo, radius = radius)
        drawCircle(colors.background, radius = radius, style = Stroke(width = GridSizes.echoOutline.toPx()))
    }
}

/**
 * A part of a cell from [left], [width] wide: the attack of a note in its [note] look, drawn by its painter of
 * [painters], its held part as a band edged above and below.
 */
private fun DrawScope.drawPart(
    part: CellPart,
    left: Float,
    width: Float,
    colors: CellColors,
    note: NoteLook,
    painters: NotePainters<NotePainter>,
) {
    when (part) {
        CellPart.SILENT -> Unit

        CellPart.ATTACK -> {
            val inset = GridSizes.noteInset.toPx()
            val noteSize = Size((width - 2 * inset).coerceAtLeast(1f), (size.height - 2 * inset).coerceAtLeast(1f))
            translate(left + inset, inset) { painters.get(note, noteSize).draw(this) }
        }

        CellPart.HELD -> {
            val band = size.height * BAND_HEIGHT
            val top = (size.height - band) / 2
            drawRect(color = colors.held.band, topLeft = Offset(left, top), size = Size(width, band))
            val edge = GridSizes.heldEdge.toPx().coerceAtMost(band / 2)
            drawRect(color = colors.held.edge, topLeft = Offset(left, top), size = Size(width, edge))
            drawRect(color = colors.held.edge, topLeft = Offset(left, top + band - edge), size = Size(width, edge))
        }
    }
}
