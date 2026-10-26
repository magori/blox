package ch.magori.blox.feature.game

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.roundToPixel
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.feature.game.controls.ControlSizes
import ch.magori.blox.feature.game.sidepanel.SidePanelSizes
import ch.magori.blox.feature.game.ui.PanelSizes
import ch.magori.blox.feature.game.ui.ScreenSizes
import kotlin.math.ceil
import kotlin.math.floor

/** The board of the mode Classique: 10 × 20 cells. */
private const val BOARD_COLUMNS = 10
private const val BOARD_ROWS = 20

/** The preview of the next piece, 4 cells wide and 2 high, fits each of them. */
private const val NEXT_COLUMNS = SidePanelSizes.PREVIEW_COLUMNS
private const val NEXT_ROWS = SidePanelSizes.PREVIEW_ROWS

/** The side column fits a score of 6 digits. */
private val SidePanelWidth = 112.dp

/**
 * The height of a line of text, in sizes of its font: that of Manrope, the tallest font of the game screen, whose
 * ascent and descent (hhea) take 2132 + 600 of its 2000 units per em, 1.366, rounded up. Press Start 2P takes 1,
 * Unbounded 1.24; Bagel Fat One (1.448) draws only the logo of the home screen.
 */
private const val LINE_HEIGHT = 1.37f

/**
 * Where the game screen puts its parts, in pixels of the space given to [gameLayout]: the [board] of
 * square cells of [cellSize], the [sidePanel] (score, level, lines, and the next piece unless
 * [nextPiece] places it apart; above the board, two by two, when [scoresAbove]), and the two groups of
 * controls, [directions] (◀ ▼ ▶) and [rotations] (B A), the arrows [directionSpacing] apart. The board is framed
 * [boardFrameInset] around its cells ([boardFrame]).
 */
@Immutable
internal data class GameLayout(
    val cellSize: Float,
    val board: Rect,
    val sidePanel: Rect,
    val nextPiece: Rect?,
    val scoresAbove: Boolean,
    val directions: Rect,
    val rotations: Rect,
    val directionSpacing: Float,
    val boardFrameInset: Float,
) {
    /**
     * The frame of the board, [boardFrameInset] around its cells, in the gaps and margins around it: the cells keep
     * their size, and the frame overlaps nothing, as the gaps beside the board are wider than it.
     */
    val boardFrame: Rect get() = board.inflate(boardFrameInset)
}

/** Around the cells of the board: its frame, then the padding of its background ([GameLayout.boardFrame]). */
private val BoardFrameInset = BloxBorders.frame + 6.dp

/**
 * The layout of the game screen in [width] × [height] pixels, at [density] pixels per dp and the system font
 * scaled by [fontScale]: of the
 * portrait layout (controls below, side by side when they fit, else one group above the other) and
 * the landscape one (controls on either side), the one with the larger board, among those that fit.
 * In a window narrower than [Sizes.scoresAboveBelow], where the board beside the side column would be
 * narrower than the column, the score above the board instead, if its board is larger; its cells no larger
 * than those of the usual layout at that width, so that a wider window never has a smaller board. When none
 * fits, a window too small for the controls, the portrait layout with one group above the other, its cells
 * of one pixel at least: what goes past the space is cut.
 */
internal fun gameLayout(
    width: Float,
    height: Float,
    density: Float,
    fontScale: Float = 1f,
): GameLayout {
    val sizes = Sizes(density, fontScale, width)
    val usual = usualLayout(width, height, sizes)
    val limit = sizes.scoresAboveBelow
    if (width >= limit) return usual
    val largestCell = usualLayout(limit, height, Sizes(density, fontScale, limit)).cellSize
    return listOf(true, false)
        .map { stacked -> scoresAbove(width, height, sizes, stacked, largestCell) }
        .filter { it.cellSize > usual.cellSize }
        .largestThatFits(width, height)
        ?: usual
}

/** Under cells of this size, the game is hardly playable: the screen asks for a larger window (P2.24). */
private val SmallestCell = 8.dp

/** Around the game screen: the margins the controls are sized for (P2.5). */
internal val ScreenMargin = 24.dp

/** The top bar on one row: as high as its pause button. */
private val TopBarRow = BloxSizes.minTouchTarget

/**
 * Whether a window of [width] × [height] pixels, inside the system bars, at [density] pixels per dp, is too small to
 * play: in the space it leaves to the board (its margins, a top bar of one row and the gap under it taken off), the
 * cells of [gameLayout] would be under [SmallestCell]. Measured on the window, so that it holds whatever the screen
 * shows, paused included; and with the normal font: a window playable in the sense of this threshold stays so with
 * an enlarged font, though less comfortably, its board smaller under the larger panels of the score above it, or
 * under a top bar of two rows (P2.26).
 */
internal fun tooSmallToPlay(
    width: Float,
    height: Float,
    density: Float,
): Boolean {
    val margins = 2 * ScreenMargin.value * density
    val top = (TopBarRow + ScreenSizes.gap).value * density
    val space = gameLayout(floor(width - margins), floor(height - margins - top), density)
    return space.cellSize < SmallestCell.value * density
}

/** Of the portrait layouts and the landscape one, the one with the largest board that fits, else the portrait stacked. */
private fun usualLayout(
    width: Float,
    height: Float,
    sizes: Sizes,
): GameLayout {
    val stacked = portrait(width, height, sizes, stacked = true)
    return listOf(portrait(width, height, sizes, stacked = false), landscape(width, height, sizes), stacked)
        .largestThatFits(width, height)
        ?: stacked
}

/** Of these layouts, the one with the largest board, among those that fit in [width] × [height]. */
private fun List<GameLayout>.largestThatFits(
    width: Float,
    height: Float,
): GameLayout? = filter { it.fitsIn(width, height) }.maxByOrNull { it.cellSize }

/** Every part inside [width] × [height], none over another. */
private fun GameLayout.fitsIn(
    width: Float,
    height: Float,
): Boolean {
    val parts = listOfNotNull(board, sidePanel, nextPiece, directions, rotations)
    val space = Rect(0f, 0f, width, height)
    return parts.all { space.intersect(it) == it } &&
        parts.withIndex().none { (index, part) -> parts.drop(index + 1).any { it.overlaps(part) } }
}

/**
 * The board and the side column above the two groups of controls: side by side, or, [stacked], ◀ ▼ ▶
 * above B A, for a narrow window.
 */
private fun portrait(
    width: Float,
    height: Float,
    sizes: Sizes,
    stacked: Boolean,
): GameLayout = with(sizes) {
    val areaHeight = height - controlsHeight(stacked) - gap
    val cellSize = cellFor(width - gap, areaHeight)
    val next = nextWidth(cellSize)
    val left = ((width - (cellSize * BOARD_COLUMNS + gap + next)) / 2).roundToPixel()
    val board = board(left, areaHeight, cellSize)
    GameLayout(
        cellSize = cellSize,
        board = board,
        sidePanel = Rect(board.right + gap, board.top, board.right + gap + next, areaHeight),
        nextPiece = null,
        scoresAbove = false,
        directions = directionsBelow(height, stacked),
        rotations = rotationsBelow(width, height, stacked),
        directionSpacing = directionSpacing,
        boardFrameInset = boardFrameInset,
    )
}

/**
 * Score and level, then lines and the next piece, in two rows across the width, the board under them, the
 * two groups of controls below: without the side column, the board takes the width of a very narrow window. Its
 * cells are [largestCell] at most.
 */
private fun scoresAbove(
    width: Float,
    height: Float,
    sizes: Sizes,
    stacked: Boolean,
    largestCell: Float,
): GameLayout = with(sizes) {
    val areaHeight = height - controlsHeight(stacked) - gap
    val cellSize = minOf(cellUnderScores(width, areaHeight), largestCell)
    val scores = scoresHeight(cellSize)
    val top = ((areaHeight - (scores + gap + cellSize * BOARD_ROWS)) / 2).roundToPixel()
    val boardTop = (top + scores + gap).roundToPixel()
    val left = ((width - cellSize * BOARD_COLUMNS) / 2).roundToPixel()
    GameLayout(
        cellSize = cellSize,
        board = Rect(left, boardTop, left + cellSize * BOARD_COLUMNS, boardTop + cellSize * BOARD_ROWS),
        sidePanel = Rect(0f, top, width, top + scores),
        nextPiece = null,
        scoresAbove = true,
        directions = directionsBelow(height, stacked),
        rotations = rotationsBelow(width, height, stacked),
        directionSpacing = directionSpacing,
        boardFrameInset = boardFrameInset,
    )
}

/**
 * ◀ ▼ ▶ at the bottom left, B A at the bottom right, and between them the score, level and lines,
 * the board and the next piece, as high as the space: panels on both sides of the board, as on the
 * original console.
 */
private fun landscape(
    width: Float,
    height: Float,
    sizes: Sizes,
): GameLayout = with(sizes) {
    val spanLeft = directionsWidth + gap
    val spanRight = width - rotationsWidth - gap
    val cellSize = cellFor(spanRight - spanLeft - side - gap - gap, height)
    val next = nextWidth(cellSize)
    val panels = side + gap + gap + next
    val left = (spanLeft + (spanRight - spanLeft - (panels + cellSize * BOARD_COLUMNS)) / 2).roundToPixel()
    val board = board(left + side + gap, height, cellSize)
    GameLayout(
        cellSize = cellSize,
        board = board,
        sidePanel = Rect(left, board.top, left + side, height),
        nextPiece = Rect(board.right + gap, board.top, board.right + gap + next, height),
        scoresAbove = false,
        directions = directionsBelow(height, stacked = false),
        rotations = rotationsBelow(width, height, stacked = false),
        directionSpacing = directionSpacing,
        boardFrameInset = boardFrameInset,
    )
}

/**
 * The sizes of the layout, in pixels. The controls are measured as Compose measures them: each button
 * and each spacing rounded to whole pixels, so that their rectangles hold them whole.
 */
private class Sizes(density: Float, fontScale: Float, width: Float) {
    private val pixels = Density(density, fontScale)

    private fun Dp.px(): Float = with(pixels) { toPx() }

    /** A line of text of this size, as the system font scales it: less than its scale for a large text. */
    private fun TextUnit.linePx(): Float = with(pixels) { toPx() } * LINE_HEIGHT

    private fun Dp.wholePx(): Float = with(pixels) { roundToPx().toFloat() }

    val gap = ScreenSizes.gap.px()
    val side = SidePanelWidth.px()
    val boardFrameInset = BoardFrameInset.wholePx()

    /** The narrowest width where the board beside the side column, as wide as the column, needs no score above it. */
    val scoresAboveBelow = gap + 2 * side

    private val spacing = ControlSizes.spacing.wholePx()
    private val directionButton = ControlSizes.directionButton.wholePx()
    private val rotationButton = ControlSizes.rotationButton.wholePx()

    /** Between two arrows: closer in a window narrower than them, down to touching. B A keep their spacing. */
    val directionSpacing = minOf(spacing, floor((width - 3 * directionButton) / 2)).coerceAtLeast(0f)
    val directionsWidth = 3 * directionButton + 2 * directionSpacing
    private val directionsHeight = (ControlSizes.rotationStagger / 2).wholePx() + directionButton
    val rotationsWidth = 2 * rotationButton + spacing
    private val rotationsHeight = ControlSizes.rotationStagger.wholePx() + rotationButton

    /** Around the preview of the next piece: the frame and the padding of its panel, on both sides. */
    private val nextInsets = (BloxBorders.frame + PanelSizes.padding).px() * 2

    /**
     * A panel of the score above the board, in whole pixels: on both sides its frame and padding, between them its
     * label, the gap under it and its value; and the same without the value, for the next piece.
     */
    private val scoreLabel = ceil(PanelSizes.label.linePx() + PanelSizes.labelGap.px() + nextInsets)
    private val scorePanel = ceil(scoreLabel + PanelSizes.value.linePx())
    private val scoreGap = SidePanelSizes.gap.px()

    /** The two groups of controls below the board: side by side, their tops level, or [stacked], ◀ ▼ ▶ above B A. */
    fun controlsHeight(stacked: Boolean): Float =
        if (stacked) directionsHeight + spacing + rotationsHeight else maxOf(directionsHeight, rotationsHeight)

    /**
     * The largest cell, in whole pixels, for a board of [height] and, beside it in [width], the
     * column of the next piece ([nextWidth]); one pixel at least, when nothing fits: the rest is then cut.
     */
    fun cellFor(
        width: Float,
        height: Float,
    ): Float = floor(
        minOf(
            (width - side) / BOARD_COLUMNS,
            (width - nextInsets) / (BOARD_COLUMNS + NEXT_COLUMNS),
            height / BOARD_ROWS,
        ),
    ).coerceAtLeast(1f)

    /**
     * The largest cell, in whole pixels, for a board as wide as [width] under the two rows of the score
     * ([scoresHeight]), in [height]; the next piece in half the width. One pixel at least.
     */
    fun cellUnderScores(
        width: Float,
        height: Float,
    ): Float {
        val board = height - scorePanel - scoreGap - gap
        return floor(
            minOf(
                width / BOARD_COLUMNS,
                ((width - scoreGap) / 2 - nextInsets) / NEXT_COLUMNS,
                (board - scorePanel) / BOARD_ROWS,
                (board - scoreLabel) / (BOARD_ROWS + NEXT_ROWS),
            ),
        ).coerceAtLeast(1f)
    }

    /** The two rows of the score above the board, the second as high as the next piece of [cellSize] needs. */
    fun scoresHeight(cellSize: Float): Float =
        scorePanel + scoreGap + maxOf(scorePanel, scoreLabel + NEXT_ROWS * cellSize)

    /** The column of the next piece: as wide as the side column, or as its 4 cells of [cellSize]. */
    fun nextWidth(cellSize: Float): Float = maxOf(side, NEXT_COLUMNS * cellSize + nextInsets)

    /** The board of [cellSize] from [left], centred in the [height] of its area. */
    fun board(
        left: Float,
        height: Float,
        cellSize: Float,
    ): Rect {
        val top = ((height - cellSize * BOARD_ROWS) / 2).roundToPixel()
        return Rect(left, top, left + cellSize * BOARD_COLUMNS, top + cellSize * BOARD_ROWS)
    }

    /** ◀ ▼ ▶ at the bottom left of [height], above B A when [stacked]. */
    fun directionsBelow(
        height: Float,
        stacked: Boolean,
    ): Rect {
        val top = height - controlsHeight(stacked)
        return Rect(0f, top, directionsWidth, top + directionsHeight)
    }

    /** B A at the bottom right of [width] × [height]. */
    fun rotationsBelow(
        width: Float,
        height: Float,
        stacked: Boolean,
    ): Rect {
        val top = height - if (stacked) rotationsHeight else controlsHeight(stacked = false)
        return Rect(width - rotationsWidth, top, width, top + rotationsHeight)
    }
}
