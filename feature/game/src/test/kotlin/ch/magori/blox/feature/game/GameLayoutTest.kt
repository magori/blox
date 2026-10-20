package ch.magori.blox.feature.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.feature.game.controls.ControlSizes
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

        // Then cells of 18 dp, the board and the side column centred above the two groups of controls
        assertThat(layout.cellSize).isEqualTo(36f)
        assertThat(layout.board).isEqualTo(Rect(left = 4f, top = 72f, right = 364f, bottom = 792f))
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 396f, top = 72f, right = 620f, bottom = 864f))
        assertThat(layout.nextPiece).isNull()
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 896f, right = 344f, bottom = 1024f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 384f, top = 896f, right = 624f, bottom = 1056f))
    }

    @Test
    fun `in landscape the controls are on either side, the score left of the board, the next piece right of it`() {
        // Given a 800 × 360 dp phone held sideways, at 2 px per dp
        val (width, height) = available(widthDp = 800, heightDp = 360, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then cells of 12 dp (7.5 in the portrait layout of this window): the board as high as the space, the
        // controls at the bottom corners
        assertThat(layout.cellSize).isEqualTo(24f)
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 428f, top = 8f, right = 652f, bottom = 496f))
        assertThat(layout.board).isEqualTo(Rect(left = 684f, top = 8f, right = 924f, bottom = 488f))
        assertThat(layout.nextPiece).isEqualTo(Rect(left = 956f, top = 8f, right = 1180f, bottom = 496f))
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 336f, right = 344f, bottom = 464f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 1264f, top = 336f, right = 1504f, bottom = 496f))
    }

    @Test
    fun `a tablet in landscape gets the landscape layout, with a larger board`() {
        // Given a 1280 × 800 dp tablet, at 2 px per dp
        val (width, height) = available(widthDp = 1280, heightDp = 800, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then cells of 34 dp, the board as high as the space
        assertThat(layout.cellSize).isEqualTo(68f)
        assertThat(layout.board.height).isEqualTo(1360f)
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

        // Then the arrows above, B A below on the right, 8 dp apart; above them the score, level, lines and next
        // piece two by two, the board under them: cells of 10 dp
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 752f, right = 344f, bottom = 880f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 104f, top = 896f, right = 344f, bottom = 1056f))
        assertThat(layout.cellSize).isEqualTo(20f)
        assertThat(layout.scoresAbove).isTrue()
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 0f, top = 9f, right = 344f, bottom = 279f))
        assertThat(layout.board).isEqualTo(Rect(left = 72f, top = 311f, right = 272f, bottom = 711f))
        assertThat(layout.nextPiece).isNull()
    }

    @ParameterizedTest(name = "at {0} px per dp, cells of {1} px")
    @CsvSource("2, 36", "2.625, 48", "3.5, 64")
    fun `a phone in portrait keeps the side column beside the board, though the score above would give a larger one`(
        density: Float,
        cellSize: Float,
    ) {
        // Given a 360 × 760 dp phone: the board is wider than the side column
        val (width, height) = available(widthDp = 360, heightDp = 760, density = density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the portrait layout, its controls side by side, the keys 8 dp apart
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
        // value of 18 sp at 30 dp (not 36). Cells of 7 dp under them, 10 dp with the normal font
        assertThat(layout.scoresAbove).isTrue()
        assertThat(layout.cellSize).isEqualTo(14f)
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 0f, top = 7f, right = 344f, bottom = 401f))
        assertThat(layout.board).isEqualTo(Rect(left = 102f, top = 433f, right = 242f, bottom = 713f))
    }

    @Test
    fun `when the score above gives a board of the same size, the usual layout stays`() {
        // Given a window of 220 × 516 dp at 2 px per dp: cells of 8 px with the score beside the board or above it
        val (width, height) = available(widthDp = 220, heightDp = 516, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then
        assertThat(layout.cellSize).isEqualTo(8f)
        assertThat(layout.scoresAbove).isFalse()
    }

    @Test
    fun `when the score above does not fit either, the usual layout stays, cut`() {
        // Given a window of 200 × 900 dp at 2 px per dp: too narrow for the arrows, in every layout; the score above
        // would give cells of 22 px, beside the side column 4 px
        val (width, height) = available(widthDp = 200, heightDp = 900, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then the portrait layout, B A under the arrows, which go past the width
        assertThat(layout.scoresAbove).isFalse()
        assertThat(layout.cellSize).isEqualTo(4f)
        assertThat(layout.rotations.top).isGreaterThanOrEqualTo(layout.directions.bottom)
        assertThat(layout.directions.right).isGreaterThan(width)
    }

    @Test
    fun `in the narrowest window at 2,625 px per dp the arrows come closer, to fit its width`() {
        // Given a window of 220 dp at 2.625 px per dp: 451 px wide, for arrows of 3 × 137 px, 21 px apart (453 px)
        val (width, height) = available(widthDp = 220, heightDp = 640, density = 2.625f)

        // When
        val layout = gameLayout(width, height, density = 2.625f)

        // Then the arrows 20 px apart, as wide as the space, B A still 8 dp apart (21 px), the score above the board
        assertThat(width).isEqualTo(451f)
        assertThat(layout.directionSpacing).isEqualTo(20f)
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 986f, right = 451f, bottom = 1155f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 136f, top = 1176f, right = 451f, bottom = 1386f))
        assertThat(layout.scoresAbove).isTrue()
        assertThat(layout.cellSize).isEqualTo(27f)
    }

    @ParameterizedTest(name = "at {0} px per dp")
    @CsvSource("1.5", "2.625", "3.5")
    fun `a window too narrow for the arrows even touching puts them touching, cut at the right`(density: Float) {
        // Given a window of 200 dp: 152 dp of space, for 3 arrows of 52 dp
        val (width, height) = available(widthDp = 200, heightDp = 640, density = density)

        // When
        val layout = gameLayout(width, height, density)

        // Then the arrows touch and go past the width, B A under them, 8 dp apart, up to the right edge
        assertThat(layout.directionSpacing).isZero()
        assertThat(layout.directions.left).isZero()
        assertThat(layout.directions.right).isGreaterThan(width)
        assertThat(layout.rotations.right).isEqualTo(width)
        assertThat(layout.rotations.width).isEqualTo(2 * Math.round(56 * density) + Math.round(8 * density).toFloat())
        assertThat(layout.scoresAbove).isFalse()
    }

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windowsWithRoom")
    fun `nothing goes past the space given, and nothing overlaps`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

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
        // the keys 8 dp apart, the arrows closer only when they would not fit the width
        with(Density(density)) {
            val direction = ControlSizes.directionButton.roundToPx()
            val rotation = ControlSizes.rotationButton.roundToPx()
            val spacing = ControlSizes.spacing.roundToPx()
            val arrowSpacing = minOf(spacing, (width.toInt() - 3 * direction) / 2).coerceAtLeast(0)
            assertThat(layout.directionSpacing).isEqualTo(arrowSpacing.toFloat())
            assertThat(layout.directions.width).isEqualTo((3 * direction + 2 * arrowSpacing).toFloat())
            assertThat(layout.directions.height)
                .isEqualTo(((ControlSizes.rotationStagger / 2).roundToPx() + direction).toFloat())
            assertThat(layout.rotations.width).isEqualTo((2 * rotation + spacing).toFloat())
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

    @ParameterizedTest(name = "{0} × {1} dp at {2} px per dp")
    @MethodSource("windows")
    fun `each group of controls holds to its edge, within the 200 dp of gesture exclusion honoured per edge`(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ) {
        // Given
        val (width, height) = available(widthDp, heightDp, density)

        // When
        val layout = gameLayout(width, height, density)

        // Then ◀ ▼ ▶ on the left edge, B A on the right one, in every layout, each excluded whole from the
        // system gestures (Back): the system ignores what goes past 200 dp on an edge
        assertThat(layout.directions.left).isEqualTo(0f)
        assertThat(layout.directions.height / density).isLessThanOrEqualTo(200f)
        assertThat(layout.rotations.right).isEqualTo(width)
        assertThat(layout.rotations.height / density).isLessThanOrEqualTo(200f)
    }

    @ParameterizedTest(name = "at {0} px per dp")
    @CsvSource("1.5", "2", "2.625", "3.5")
    fun `the narrowest multi-window is too small to play 590 dp high, its cells under 8 dp, and playable 592 dp high`(
        density: Float,
    ) {
        // Given windows of 220 dp wide, 590 and 592 dp high (591 at 2 px per dp and more): the score above the board
        val (width, lower) = available(widthDp = 220, heightDp = 590, density = density)
        val (_, higher) = available(widthDp = 220, heightDp = 592, density = density)

        // When / Then cells under 8 dp in the lower, too small to play; 8 dp at least in the higher. The window is
        // measured whole, its margins and its top bar of one row (48 dp) taken off
        assertThat(gameLayout(width, lower, density).cellSize).isLessThan(8 * density)
        assertThat(tooSmallToPlay(220 * density, 590 * density, density)).isTrue()
        assertThat(gameLayout(width, higher, density).cellSize).isGreaterThanOrEqualTo(8 * density)
        assertThat(tooSmallToPlay(220 * density, 592 * density, density)).isFalse()
    }

    @ParameterizedTest(name = "at {0} px per dp")
    @CsvSource("1.5", "2", "2.625", "3.5")
    fun `a window too narrow for the arrows is too small to play, however high`(density: Float) {
        // Given a window of 200 × 1200 dp: the arrows go past its width, the board beside the side column

        // When / Then
        assertThat(tooSmallToPlay(200 * density, 1200 * density, density)).isTrue()
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
     * The space a window of [widthDp] × [heightDp] leaves to [gameLayout], in pixels: without the 24 dp
     * margins, the top bar (48 dp, its pause button) and the 16 dp under it. Whole pixels, rounded down,
     * as the constraints Compose gives.
     */
    private fun available(
        widthDp: Int,
        heightDp: Int,
        density: Float,
    ): Pair<Float, Float> = floor((widthDp - 2 * 24) * density) to floor((heightDp - 2 * 24 - 48 - 16) * density)

    companion object {
        private val densities = listOf(1.5f, 2.625f, 3.5f)

        /**
         * A phone, the same sideways, a tablet both ways, narrow multi-windows down to the narrowest, 220 dp, at
         * usual densities: everything fits.
         */
        @JvmStatic
        fun windowsWithRoom(): List<Arguments> =
            listOf(360 to 640, 360 to 760, 800 to 360, 1280 to 800, 800 to 1280, 260 to 640, 220 to 640)
                .at(densities)
                .arguments()

        /** Windows too small for the controls, where the rest is cut: too low, too narrow. */
        @JvmStatic
        fun windowsTooSmall(): List<Arguments> = listOf(360 to 200, 220 to 220, 200 to 640).at(densities).arguments()

        /** Every window, with room or not. */
        @JvmStatic
        fun windows(): List<Arguments> = windowsWithRoom() + windowsTooSmall()

        private fun List<Pair<Int, Int>>.at(densities: List<Float>): List<Triple<Int, Int, Float>> =
            flatMap { (width, height) -> densities.map { Triple(width, height, it) } }

        private fun List<Triple<Int, Int, Float>>.arguments(): List<Arguments> =
            map { (width, height, density) -> Arguments.of(width, height, density) }
    }
}
