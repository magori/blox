package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The shapes of the interface, the `radius-*` tokens of docs/design-system/tokens.json (ShapeTokensTest): the same day
 * and night, square in the Original theme.
 */
@Immutable
data class BloxShapes(
    /** A cell of a piece of 16 dp and more (`radius-cell`). */
    val cell: Shape,
    /** Panels, arrows, tiles, a pressed row, the board on a phone (`radius-md`). */
    val md: Shape,
    /** A message that goes away, the snackbar (`radius-snack`). */
    val snack: Shape,
    /** The card of the settings of the home, the board on a tablet (`radius-window`). */
    val window: Shape,
    /** The cards of the pause and of the end of a game, the dialog windows (`radius-card`). */
    val card: Shape,
    /** Buttons of a screen, groups of choices, stickers; a disc on a square (`radius-pill`). */
    val pill: Shape,
)

object BloxShapeSets {
    /** The Original theme: square corners. */
    val Original = BloxShapes(
        cell = RectangleShape,
        md = RectangleShape,
        snack = RectangleShape,
        window = RectangleShape,
        card = RectangleShape,
        pill = RectangleShape,
    )

    /** The Colorful theme: the radii of tokens json. */
    val Colorful = BloxShapes(
        cell = RoundedCornerShape(6.dp),
        md = RoundedCornerShape(16.dp),
        snack = RoundedCornerShape(20.dp),
        window = RoundedCornerShape(24.dp),
        card = RoundedCornerShape(28.dp),
        pill = RoundedCornerShape(percent = 50),
    )
}

/** The widths of the strokes of the interface, the same in both themes. */
object BloxBorders {
    /** The frame of a panel, of a secondary button, of a choice of the home and of a key of the game. */
    val frame = 2.dp
}
