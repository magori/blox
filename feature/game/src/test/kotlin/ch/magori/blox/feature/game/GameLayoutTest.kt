package ch.magori.blox.feature.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.feature.game.controls.ControlSizes
import ch.magori.blox.feature.game.controls.KeySizes
import ch.magori.blox.feature.game.controls.keySizes
import ch.magori.blox.feature.game.ui.PanelSizes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.floor

class GameLayoutTest {
    @Test
    fun `in portrait the board is on the left, the side column on its right and the controls below`() {
        // Given a 360 × 640 dp phone, at 2 px per dp
        val (width, height) = available(widthDp = 360, heightDp = 640, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then cells of 19 dp, the board in its frame and the side column centred above the two groups of controls, of
        // compact keys: ◀ ▼ ▶ 52 + 17,33 dp high (139 px), B A 56 + 24 dp
        assertThat(layout.cellSize).isEqualTo(38f)
        assertThat(layout.board).isEqualTo(Rect(left = 18f, top = 68f, right = 398f, bottom = 828f))
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 430f, top = 68f, right = 654f, bottom = 896f))
        assertThat(layout.nextPiece).isNull()
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 928f, right = 344f, bottom = 1067f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 400f, top = 928f, right = 656f, bottom = 1088f))
    }

    @Test
    fun `left handed puts B and A on the left and the arrows on the right`() {
        // Given the same phone, for a left handed player
        val (width, height) = available(widthDp = 360, heightDp = 640, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f, leftHanded = true)

        // Then the two groups swap sides, as in a mirror; the board and the side column stay where they are
        val rightHanded = gameLayout(width, height, density = 2f)
        assertThat(layout.rotations).isEqualTo(Rect(left = 0f, top = 928f, right = 256f, bottom = 1088f))
        assertThat(layout.directions).isEqualTo(Rect(left = 312f, top = 928f, right = 656f, bottom = 1067f))
        assertThat(layout.board).isEqualTo(rightHanded.board)
        assertThat(layout.sidePanel).isEqualTo(rightHanded.sidePanel)
        assertThat(layout.leftHanded).isTrue()
        assertThat(rightHanded.leftHanded).isFalse()
    }

    @ParameterizedTest(name = "{0} dp wide at {1} px per dp")
    @CsvSource("412, 2.625", "390, 3", "360, 2", "220, 2")
    fun `the keys are the largest whose two groups fit the width side by side`(
        widthDp: Int,
        density: Float,
    ) {
        // Given a window high enough for its board to take the width
        val (width, height) = available(widthDp, heightDp = 800, density = density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the keys that the width of the space chooses
        assertThat(layout.keys).isEqualTo(keySizes((widthDp - 2 * 16).dp))
    }

    @Test
    fun `a window too low for the largest keys takes smaller ones, for a larger board`() {
        // Given a window of 412 × 640 dp at 2 px per dp, wide enough for the keys of a phone: under them, the board
        // would have cells of 21,5 dp
        val (width, height) = available(widthDp = 412, heightDp = 640, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then the keys halfway, 6 dp lower, and cells of 22 dp
        assertThat(layout.keys).isEqualTo(KeySizes.Halfway)
        assertThat(layout.cellSize).isEqualTo(44f)
    }

    @Test
    fun `in landscape the controls are on either side, the score left of the board, the next piece right of it`() {
        // Given a 800 × 360 dp phone held sideways, at 2 px per dp
        val (width, height) = available(widthDp = 800, heightDp = 360, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then cells of 13 dp: the board as high as the space, the controls of a phone at the bottom corners
        assertThat(layout.cellSize).isEqualTo(26f)
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 426f, top = 4f, right = 650f, bottom = 528f))
        assertThat(layout.board).isEqualTo(Rect(left = 682f, top = 4f, right = 942f, bottom = 524f))
        assertThat(layout.nextPiece).isEqualTo(Rect(left = 974f, top = 4f, right = 1198f, bottom = 528f))
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 344f, right = 392f, bottom = 504f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 1232f, top = 344f, right = 1536f, bottom = 528f))
    }

    @Test
    fun `in landscape, left handed, the board stays centred between the two groups`() {
        // Given the same phone held sideways, for a left handed player: B A, narrower than the arrows, on the left
        val (width, height) = available(widthDp = 800, heightDp = 360, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f, leftHanded = true)

        // Then the score, the board and the next piece, as wide as the score, are where they would be in a mirror
        val rightHanded = gameLayout(width, height, density = 2f)
        assertThat(layout.cellSize).isEqualTo(rightHanded.cellSize)
        assertThat(layout.board.left).isEqualTo(width - rightHanded.board.right)
        assertThat(layout.rotations.left).isZero()
        assertThat(layout.directions.right).isEqualTo(width)
    }

    @Test
    fun `a tablet in landscape gets the landscape layout, with a larger board`() {
        // Given a 1280 × 800 dp tablet, at 2 px per dp
        val (width, height) = available(widthDp = 1280, heightDp = 800, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then cells of 35 dp, the board as high as the space
        assertThat(layout.cellSize).isEqualTo(70f)
        assertThat(layout.board.height).isEqualTo(1400f)
        assertThat(layout.nextPiece).isNotNull()
    }

    @Test
    fun `a tablet in portrait keeps the portrait layout`() {
        // Given a 800 × 1280 dp tablet, at 2 px per dp
        val (width, height) = available(widthDp = 800, heightDp = 1280, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then the controls below the board, side by side, and the next piece in the side column
        assertThat(layout.nextPiece).isNull()
        assertThat(layout.directions.top).isGreaterThanOrEqualTo(layout.board.bottom)
        assertThat(layout.rotations.top).isEqualTo(layout.directions.top)
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windowsTooSmall")
    fun `a window too small for the controls puts B A under the arrows, cut rather than one over the other`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the arrows from the left, B A under them up to the right, the board of one pixel cells at least
        assertThat(layout.directions.left).isZero()
        assertThat(layout.rotations.top).isGreaterThanOrEqualTo(layout.directions.bottom)
        assertThat(layout.rotations.right).isEqualTo(width)
        assertThat(layout.cellSize).isGreaterThanOrEqualTo(1f)
    }

    @Test
    fun `a very narrow window puts the score above the board, for a board larger than beside the side column`() {
        // Given a window of 220 dp, the narrowest in multi-window, at 2 px per dp: beside the side column, the
        // board would have cells of 4 dp
        val (width, height) = available(widthDp = 220, heightDp = 640, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then the arrows above, B A below on the right, 16 dp apart; above them the score, level, lines and next
        // piece two by two, the board under them: cells of 10 dp
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 773f, right = 344f, bottom = 912f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 120f, top = 928f, right = 376f, bottom = 1088f))
        assertThat(layout.cellSize).isEqualTo(20f)
        assertThat(layout.scoresAbove).isTrue()
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 0f, top = 18f, right = 376f, bottom = 292f))
        assertThat(layout.board).isEqualTo(Rect(left = 88f, top = 324f, right = 288f, bottom = 724f))
        assertThat(layout.nextPiece).isNull()
    }

    @ParameterizedTest(name = "at {0} px per dp, cells of {1} px")
    @CsvSource("2, 38", "2.625, 50", "3.5, 67")
    fun `a phone in portrait keeps the side column beside the board, though the score above would give a larger one`(
        density: Float,
        cellSize: Float,
    ) {
        // Given a 360 × 760 dp phone: the board is wider than the side column
        val (width, height) = available(widthDp = 360, heightDp = 760, density = density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the portrait layout, its controls side by side, the arrows 8 dp apart
        assertThat(layout.scoresAbove).isFalse()
        assertThat(layout.nextPiece).isNull()
        assertThat(layout.cellSize).isEqualTo(cellSize)
        assertThat(layout.sidePanel.left).isGreaterThan(layout.board.right)
        assertThat(layout.rotations.top).isEqualTo(layout.directions.top)
        assertThat(layout.directionSpacing).isEqualTo(8 * density)
    }

    @ParameterizedTest(name = "{0} dp high at {1} px per dp")
    @CsvSource("640, 2", "640, 2.625", "760, 2", "760, 2.625", "900, 2", "900, 2.625", "1200, 2.625")
    fun `the board never shrinks as the window widens, from the narrowest multi-window to a phone`(
        heightDp: Int,
        density: Float,
    ) {
        // Given windows of 220 to 420 dp wide, dp by dp: the score above the board in the narrowest, the side
        // column beside it in the widest
        val cells = (220..420).associateWith { widthDp ->
            val (width, height) = available(widthDp, heightDp, density)
            gameLayout(width, height, density).cellSize
        }

        // When and Then: each window has cells as large as the one 1 dp narrower, at least
        cells.entries.zipWithNext().forEach { (narrower, wider) ->
            assertThat(wider.value)
                .describedAs("%d dp: %s px, %d dp: %s px", narrower.key, narrower.value, wider.key, wider.value)
                .isGreaterThanOrEqualTo(narrower.value)
        }
    }

    @Test
    fun `with the font twice as large, the score above reserves the height of its larger texts`() {
        // Given the narrowest window at 2 px per dp, with the system font twice as large
        val (width, height) = available(widthDp = 220, heightDp = 640, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f, fontScale = 2f)

        // Then rows for the texts as the system scales them, less for the larger: a label of 11 sp drawn at 22 dp, a
        // value of 18 sp at 30 dp (not 36). Cells of 7,5 dp under them, 10 dp with the normal font
        assertThat(layout.scoresAbove).isTrue()
        assertThat(layout.cellSize).isEqualTo(15f)
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 0f, top = 5f, right = 376f, bottom = 405f))
        assertThat(layout.board).isEqualTo(Rect(left = 113f, top = 437f, right = 263f, bottom = 737f))
    }

    @Test
    fun `when the score above gives a board of the same size, the usual layout stays`() {
        // Given a window of 220 × 528 dp at 2 px per dp: cells of 10 px with the score beside the board or above it
        val (width, height) = available(widthDp = 220, heightDp = 528, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then
        assertThat(layout.cellSize).isEqualTo(10f)
        assertThat(layout.scoresAbove).isFalse()
    }

    @Test
    fun `when the score above does not fit either, the usual layout stays, cut`() {
        // Given a window of 184 × 900 dp at 2 px per dp: too narrow for the arrows, in every layout; the score above
        // would give larger cells than the 3 px beside the side column
        val (width, height) = available(widthDp = 184, heightDp = 900, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then the portrait layout, B A under the arrows, which go past the width
        assertThat(layout.scoresAbove).isFalse()
        assertThat(layout.cellSize).isEqualTo(3f)
        assertThat(layout.rotations.top).isGreaterThanOrEqualTo(layout.directions.bottom)
        assertThat(layout.directions.right).isGreaterThan(width)
    }

    @Test
    fun `in a window of 200 dp at 2,625 px per dp the arrows come closer, to fit its width`() {
        // Given a window of 200 dp at 2.625 px per dp: 441 px wide, for arrows of 3 × 137 px, 21 px apart (453 px)
        val (width, height) = available(widthDp = 200, heightDp = 640, density = 2.625f)

        // When
        val layout = gameLayout(width, height, density = 2.625f)

        // Then the arrows 15 px apart, as wide as the space, B A still 16 dp apart (42 px), the score above the board
        assertThat(width).isEqualTo(441f)
        assertThat(layout.directionSpacing).isEqualTo(15f)
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 1014f, right = 441f, bottom = 1197f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 105f, top = 1218f, right = 441f, bottom = 1428f))
        assertThat(layout.scoresAbove).isTrue()
        assertThat(layout.cellSize).isEqualTo(27f)
    }

    @ParameterizedTest(name = "at {0} px per dp")
    @CsvSource("1.5", "2.625", "3.5")
    fun `a window too narrow for the arrows even touching puts them touching, cut at the right`(density: Float) {
        // Given a window of 184 dp: 152 dp of space, for 3 arrows of 52 dp
        val (width, height) = available(widthDp = 184, heightDp = 640, density = density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the arrows touch and go past the width, B A under them, 16 dp apart, up to the right edge
        assertThat(layout.directionSpacing).isZero()
        assertThat(layout.directions.left).isZero()
        assertThat(layout.directions.right).isGreaterThan(width)
        assertThat(layout.rotations.right).isEqualTo(width)
        assertThat(layout.rotations.width).isEqualTo(2 * Math.round(56 * density) + Math.round(16 * density).toFloat())
        assertThat(layout.scoresAbove).isFalse()
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp, left handed: {3}")
    @MethodSource("windowsWithRoomEitherHand")
    fun `nothing goes past the space given, and nothing overlaps`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
        leftHanded: Boolean,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density, leftHanded = leftHanded)

        // Then
        val parts = listOfNotNull(layout.board, layout.sidePanel, layout.nextPiece, layout.directions, layout.rotations)
        val space = Rect(0f, 0f, width, height)
        parts.forEach { part -> assertThat(space.intersect(part)).describedAs("$part").isEqualTo(part) }
        parts.forEachIndexed { index, part ->
            parts.drop(index + 1).forEach { other ->
                assertThat(part.overlaps(other)).describedAs("$part, $other").isFalse()
            }
        }
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windowsWithRoom")
    fun `the board is framed by 8 dp all around, clear of every other part, within the margins and the gaps`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the frame takes 8 dp, in whole pixels, around the cells: out of the 16 dp of the gaps beside the board
        // and of the 16 dp of the margins, never over the score, the next piece nor the controls
        val inset = Math.round(8 * density).toFloat()
        val frame = layout.boardFrame
        assertThat(frame).isEqualTo(layout.board.inflate(inset))
        assertThat(Rect(0f, 0f, width, height).inflate(16 * density).intersect(frame)).isEqualTo(frame)
        listOfNotNull(layout.sidePanel, layout.nextPiece, layout.directions, layout.rotations).forEach { part ->
            assertThat(frame.overlaps(part)).describedAs("$frame, $part").isFalse()
        }
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windowsWithRoom")
    fun `the frame of the board stays within the width of the space, lined up with the top bar and the controls`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the frame does not pass the pause button nor the arrows on the left, nor B A on the right, into the margins
        assertThat(layout.boardFrame.left).isGreaterThanOrEqualTo(0f)
        assertThat(layout.boardFrame.right).isLessThanOrEqualTo(width)
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windows")
    fun `the board has 10 × 20 square cells of whole pixels, from a whole pixel`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

        // Then
        assertThat(layout.cellSize).isGreaterThanOrEqualTo(1f)
        assertThat(layout.cellSize % 1f).isZero()
        assertThat(layout.board.width).isEqualTo(10 * layout.cellSize)
        assertThat(layout.board.height).isEqualTo(20 * layout.cellSize)
        assertThat(layout.board.left % 1f).isZero()
        assertThat(layout.board.top % 1f).isZero()
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windows")
    fun `the next piece has room for four cells of the board size, inside its panel`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

        // Then: 4 cells, and on each side the frame and the padding of the panel
        val panel = layout.nextPiece ?: layout.sidePanel
        val insets = 2 * (BloxBorders.frame + PanelSizes.padding).value * density
        assertThat(panel.width).isGreaterThanOrEqualTo(4 * layout.cellSize + insets)
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windows")
    fun `the controls are placed whole, as Compose measures them, with buttons of at least 48 dp`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

        // Then each group has the size of its row, every button and spacing rounded to whole pixels as Compose does;
        // the arrows 8 dp apart, closer only when they would not fit the width, ▼ a third of a key lower, B A 16 dp apart
        with(Density(density)) {
            val direction = layout.keys.arrow.roundToPx()
            val rotation = layout.keys.rotation.roundToPx()
            val spacing = ControlSizes.spacing.roundToPx()
            val arrowSpacing = minOf(spacing, (width.toInt() - 3 * direction) / 2).coerceAtLeast(0)
            assertThat(layout.directionSpacing).isEqualTo(arrowSpacing.toFloat())
            assertThat(layout.directions.width).isEqualTo((3 * direction + 2 * arrowSpacing).toFloat())
            assertThat(layout.directions.height)
                .isEqualTo((layout.keys.arrowRaise.roundToPx() + direction).toFloat())
            assertThat(
                layout.rotations.width,
            ).isEqualTo((2 * rotation + ControlSizes.rotationSpacing.roundToPx()).toFloat())
            assertThat(
                layout.rotations.height,
            ).isEqualTo((ControlSizes.rotationStagger.roundToPx() + rotation).toFloat())
        }
        // and the buttons in the rectangles placed are 48 dp at least
        assertThat(layout.directions.width / density / 3).isGreaterThanOrEqualTo(48f)
        assertThat(layout.directions.height / density).isGreaterThanOrEqualTo(48f)
        assertThat(layout.rotations.width / density / 2).isGreaterThanOrEqualTo(48f)
        assertThat(layout.rotations.height / density).isGreaterThanOrEqualTo(48f)
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp, left handed: {3}")
    @MethodSource("windowsEitherHand")
    fun `each group of controls holds to its edge, within the 200 dp of gesture exclusion honoured per edge`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
        leftHanded: Boolean,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density, leftHanded = leftHanded)

        // Then ◀ ▼ ▶ on the left edge, B A on the right one (the other way round for the left handed), in every
        // layout, each excluded whole from the system gestures (Back): the system ignores what goes past 200 dp on an
        // edge
        val (left, right) = if (leftHanded) {
            layout.rotations to layout.directions
        } else {
            layout.directions to
                layout.rotations
        }
        assertThat(left.left).isEqualTo(0f)
        assertThat(right.right).isEqualTo(width)
        assertThat(layout.directions.height / density).isLessThanOrEqualTo(200f)
        assertThat(layout.rotations.height / density).isLessThanOrEqualTo(200f)
    }

    @ParameterizedTest(name = "at {0} px per dp")
    @CsvSource("1.5", "2", "2.625", "3.5")
    fun `the narrowest multi-window is too small to play 581 dp high, its cells under 8 dp, and playable 583 dp high`(
        density: Float,
    ) {
        // Given windows of 220 dp wide, 581 and 583 dp high (582 is enough but at 2 px per dp): the score
        // above the board
        val (width, lower) = available(widthDp = 220, heightDp = 581, density = density)
        val (_, higher) = available(widthDp = 220, heightDp = 583, density = density)

        // When / Then cells under 8 dp in the lower, too small to play; 8 dp at least in the higher. The window is
        // measured whole, its margins and its top bar of one row (48 dp) taken off
        assertThat(gameLayout(width, lower, density).cellSize).isLessThan(8 * density)
        assertThat(tooSmallToPlay(220 * density, 581 * density, density)).isTrue()
        assertThat(gameLayout(width, higher, density).cellSize).isGreaterThanOrEqualTo(8 * density)
        assertThat(tooSmallToPlay(220 * density, 583 * density, density)).isFalse()
    }

    @ParameterizedTest(name = "at {0} px per dp")
    @CsvSource("1.5", "2", "2.625", "3.5")
    fun `a window too narrow for the arrows is too small to play, however high`(density: Float) {
        // Given a window of 184 × 1200 dp: the arrows go past its width, the board beside the side column

        // When / Then
        assertThat(tooSmallToPlay(184 * density, 1200 * density, density)).isTrue()
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windowsWithRoom")
    fun `phones and tablets, in portrait and in landscape, and the narrowest multi-window are never too small to play`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // When / Then
        assertThat(tooSmallToPlay(widthDp * density, heightDp * density, density)).isFalse()
    }

    /**
     * The space a window of [widthDp] × [heightDp] leaves to [gameLayout], in pixels: without the 16 dp
     * margins, the top bar (48 dp, its pause button) and the 16 dp under it. Whole pixels, rounded down,
     * as the constraints Compose gives.
     */
    private fun available(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ): Pair<Float, Float> = floor((widthDp - 2 * 16) * density) to floor((heightDp - 2 * 16 - 48 - 16) * density)

    companion object {
        private val densities = listOf(1.5f, 2.625f, 3.5f)

        /**
         * Phones (their keys compact, halfway and of a phone), one sideways, a tablet both ways, narrow multi-windows
         * down to the narrowest, 220 dp, at usual densities: everything fits.
         */
        @JvmStatic
        fun windowsWithRoom(): List<Arguments> = listOf(
            360 to 640,
            360 to 760,
            390 to 844,
            412 to 915,
            800 to 360,
            1280 to 800,
            800 to 1280,
            260 to 640,
            220 to 640,
        ).at(densities).arguments()

        /** The windows of [windowsWithRoom], for a right handed player and for a left handed one. */
        @JvmStatic
        fun windowsWithRoomEitherHand(): List<Arguments> = windowsWithRoom().eitherHand()

        /** The windows of [windows], for a right handed player and for a left handed one. */
        @JvmStatic
        fun windowsEitherHand(): List<Arguments> = windows().eitherHand()

        private fun List<Arguments>.eitherHand(): List<Arguments> =
            flatMap { window -> listOf(false, true).map { Arguments.of(*window.get(), it) } }

        /** Windows too small for the controls, where the rest is cut: too low, too narrow. */
        @JvmStatic
        fun windowsTooSmall(): List<Arguments> = listOf(360 to 180, 220 to 220, 184 to 640).at(densities).arguments()

        /** Every window, with room or not. */
        @JvmStatic
        fun windows(): List<Arguments> = windowsWithRoom() + windowsTooSmall()

        private fun List<Pair<Int, Int>>.at(densities: List<Float>): List<Triple<Int, Int, Float>> =
            flatMap { (width, height) -> densities.map { Triple(width, height, it) } }

        private fun List<Triple<Int, Int, Float>>.arguments(): List<Arguments> =
            map { (width, height, density) -> Arguments.of(width, height, density) }
    }
}
