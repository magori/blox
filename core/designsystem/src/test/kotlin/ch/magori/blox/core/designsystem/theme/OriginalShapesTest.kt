package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.RectangleShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The Original theme keeps the flat squares of the console: no token of radius or of shadow applies to it. */
class OriginalShapesTest {
    @Test
    fun `the Original theme has square corners and no gloss`() {
        // When
        val shapes = shapesOf(BloxThemeKind.ORIGINAL)
        val glosses = glossesOf(BloxThemeVariant.ORIGINAL)

        // Then every shape and every gloss, each named so that a new one has to be added here
        assertThat(shapes).isEqualTo(
            BloxShapes(
                cell = RectangleShape,
                md = RectangleShape,
                snack = RectangleShape,
                window = RectangleShape,
                card = RectangleShape,
                pill = RectangleShape,
            ),
        )
        assertThat(glosses).isEqualTo(
            BloxGlosses(button = Gloss.None, buttonPressed = Gloss.None, cell = Gloss.None, menu = DropShadow.None),
        )
    }
}
