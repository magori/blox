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
    fun `a very narrow window puts B A under the arrows, and the board shrinks`() {
        // Given a window of 220 dp, the narrowest in multi-window, at 2 px per dp
        val (width, height) = available(widthDp = 220, heightDp = 640, density = 2f)

        // When
        val layout = gameLayout(width, height, density = 2f)

        // Then the arrows above, B A below on the right, 8 dp apart; cells of 4 dp
        assertThat(layout.directions).isEqualTo(Rect(left = 0f, top = 752f, right = 344f, bottom = 880f))
        assertThat(layout.rotations).isEqualTo(Rect(left = 104f, top = 896f, right = 344f, bottom = 1056f))
        assertThat(layout.cellSize).isEqualTo(8f)
        assertThat(layout.board).isEqualTo(Rect(left = 4f, top = 280f, right = 84f, bottom = 440f))
        assertThat(layout.sidePanel).isEqualTo(Rect(left = 116f, top = 280f, right = 340f, bottom = 720f))
        assertThat(layout.nextPiece).isNull()
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

        // Then each group has the size of its row, every button and spacing rounded to whole pixels as Compose does
        with(Density(density)) {
            val direction = ControlSizes.directionButton.roundToPx()
            val rotation = ControlSizes.rotationButton.roundToPx()
            val spacing = ControlSizes.spacing.roundToPx()
            assertThat(layout.directions.width).isEqualTo((3 * direction + 2 * spacing).toFloat())
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

        /** The narrowest multi-window at 2.625 px per dp: 451 px wide, for arrows of 453 px. */
        private val narrowest = Triple(220, 640, 2.625f)

        /**
         * A phone, the same sideways, a tablet both ways, the narrowest multi-window, at usual densities:
         * everything fits, but in the [narrowest] window.
         */
        @JvmStatic
        fun windowsWithRoom(): List<Arguments> =
            (listOf(360 to 640, 800 to 360, 1280 to 800, 800 to 1280, 220 to 640).at(densities) - narrowest).arguments()

        /** Windows too small for the controls, where the rest is cut: too low, too narrow, and the [narrowest]. */
        @JvmStatic
        fun windowsTooSmall(): List<Arguments> =
            (listOf(360 to 200, 220 to 220, 200 to 640).at(densities) + narrowest).arguments()

        /** Every window, with room or not. */
        @JvmStatic
        fun windows(): List<Arguments> = windowsWithRoom() + windowsTooSmall()

        private fun List<Pair<Int, Int>>.at(densities: List<Float>): List<Triple<Int, Int, Float>> =
            flatMap { (width, height) -> densities.map { Triple(width, height, it) } }

        private fun List<Triple<Int, Int, Float>>.arguments(): List<Arguments> =
            map { (width, height, density) -> Arguments.of(width, height, density) }
    }
}
