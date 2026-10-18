package ch.magori.blox.core.game

import ch.magori.blox.core.game.model.Position
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class ShapeDrawingTest {
    @Test
    fun `cells are read as offsets from the pivot`() {
        assertThat(
            cellsOf(
                """
                T@T
                .T.
                """,
            ),
        ).containsExactlyInAnyOrder(Position(-1, 0), Position(0, 0), Position(1, 0), Position(0, 1))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("invalidDrawings")
    fun `a drawing needs exactly one pivot, only known symbols and no blank row inside`(
        drawing: String,
        expectedMessage: String,
    ) {
        assertThatIllegalArgumentException()
            .isThrownBy { cellsOf(drawing) }
            .withMessageContaining(expectedMessage)
    }

    private companion object {
        @JvmStatic
        fun invalidDrawings(): List<Arguments> = listOf(
            argumentSet("no pivot", "TTT\n.T.", "exactly one pivot"),
            argumentSet("two pivots", "T@@\n.T.", "exactly one pivot"),
            argumentSet("an unknown symbol", "T@#\n.T.", "'#'"),
            argumentSet("a blank row inside", "T@T\n\n.T.", "blank row"),
        )
    }
}
