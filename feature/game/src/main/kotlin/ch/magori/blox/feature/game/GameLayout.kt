package ch.magori.blox.feature.game

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.feature.game.controls.ControlSizes
import ch.magori.blox.feature.game.ui.PanelSizes
import ch.magori.blox.feature.game.ui.ScreenSizes
import ch.magori.blox.feature.game.ui.roundToPixel
import kotlin.math.floor

/** The board of the mode Classique: 10 × 20 cells. */
private const val BOARD_COLUMNS = 10
private const val BOARD_ROWS = 20

/** The preview of the next piece fits the widest one, 4 cells. */
private const val NEXT_COLUMNS = 4

/** The side column fits a score of 6 digits. */
private val SidePanelWidth = 112.dp

/**
 * Where the game screen puts its parts, in pixels of the space given to [gameLayout]: the [board] of
 * square cells of [cellSize], the [sidePanel] (score, level, lines, and the next piece unless
 * [nextPiece] places it apart), and the two groups of controls, [directions] (◀ ▼ ▶) and [rotations]
 * (B A).
 */
@Immutable
internal data class GameLayout(
    val cellSize: Float,
    val board: Rect,
    val sidePanel: Rect,
    val nextPiece: Rect?,
    val directions: Rect,
    val rotations: Rect,
)

/**
 * The layout of the game screen in [width] × [height] pixels, at [density] pixels per dp: of the
 * portrait layout (controls below, side by side when they fit, else one group above the other) and
 * the landscape one (controls on either side), the one with the larger board, among those that fit.
 * When none fits, a window too small for the controls, the portrait layout with one group above the
 * other, its cells of one pixel at least: what goes past the space is cut.
 */
internal fun gameLayout(
    width: Float,
    height: Float,
    density: Float,
): GameLayout {
    val sizes = Sizes(density)
    val stacked = portrait(width, height, sizes, stacked = true)
    return listOf(portrait(width, height, sizes, stacked = false), landscape(width, height, sizes), stacked)
        .filter { it.fitsIn(width, height) }
        .maxByOrNull { it.cellSize }
        ?: stacked
}

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
    val controlsHeight = if (stacked) directionsHeight + spacing + rotationsHeight else controlsHeight
    val areaHeight = height - controlsHeight - gap
    val cellSize = cellFor(width - gap, areaHeight)
    val next = nextWidth(cellSize)
    val left = ((width - (cellSize * BOARD_COLUMNS + gap + next)) / 2).roundToPixel()
    val board = board(left, areaHeight, cellSize)
    GameLayout(
        cellSize = cellSize,
        board = board,
        sidePanel = Rect(board.right + gap, board.top, board.right + gap + next, areaHeight),
        nextPiece = null,
        directions = directions(left = 0f, top = height - controlsHeight),
        rotations = rotations(right = width, top = height - if (stacked) rotationsHeight else controlsHeight),
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
        directions = directions(left = 0f, top = height - controlsHeight),
        rotations = rotations(right = width, top = height - controlsHeight),
    )
}

/**
 * The sizes of the layout, in pixels. The controls are measured as Compose measures them: each button
 * and each spacing rounded to whole pixels, so that their rectangles hold them whole.
 */
private class Sizes(density: Float) {
    private val pixels = Density(density)

    private fun Dp.px(): Float = with(pixels) { toPx() }

    private fun Dp.wholePx(): Float = with(pixels) { roundToPx().toFloat() }

    val gap = ScreenSizes.gap.px()
    val side = SidePanelWidth.px()
    val spacing = ControlSizes.spacing.wholePx()
    private val directionButton = ControlSizes.directionButton.wholePx()
    private val rotationButton = ControlSizes.rotationButton.wholePx()
    val directionsWidth = 3 * directionButton + 2 * spacing
    val directionsHeight = (ControlSizes.rotationStagger / 2).wholePx() + directionButton
    val rotationsWidth = 2 * rotationButton + spacing
    val rotationsHeight = ControlSizes.rotationStagger.wholePx() + rotationButton

    /** The two groups side by side, their tops level. */
    val controlsHeight = maxOf(directionsHeight, rotationsHeight)

    /** Around the preview of the next piece: the frame and the padding of its panel, on both sides. */
    private val nextInsets = (PanelSizes.border + PanelSizes.padding).px() * 2

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

    /** ◀ ▼ ▶ from [left] and [top]. */
    fun directions(
        left: Float,
        top: Float,
    ): Rect = Rect(left, top, left + directionsWidth, top + directionsHeight)

    /** B A up to [right], from [top]. */
    fun rotations(
        right: Float,
        top: Float,
    ): Rect = Rect(right - rotationsWidth, top, right, top + rotationsHeight)
}
