package ch.magori.blox.core.board

import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

/** The logo of the home screen takes colours of the pieces: they are read from the pieces, written once. */
class LogoColorsTest {
    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("themes")
    fun `the logo cells of each theme are fills of its pieces`(
        variant: BloxThemeVariant,
        pieces: List<PieceType>,
    ) {
        // When
        val logo = logoColors(variant)

        // Then, in reading order
        assertThat(listOf(logo.topLeft, logo.topRight, logo.bottomLeft, logo.bottomRight))
            .containsExactlyElementsOf(pieces.map { cellStyle(variant, Cell.Filled(it)).fill })
    }

    private companion object {
        @JvmStatic
        fun themes(): List<Arguments> = listOf(
            argumentSet(
                "Original: the fills of I, O, J and T",
                BloxThemeVariant.ORIGINAL,
                listOf(PieceType.I, PieceType.O, PieceType.J, PieceType.T),
            ),
            argumentSet(
                "Colorful, day: the fills of I, O, Z and S",
                BloxThemeVariant.COLORFUL_LIGHT,
                listOf(PieceType.I, PieceType.O, PieceType.Z, PieceType.S),
            ),
            argumentSet(
                "Colorful, night: the fills of I, O, Z and S",
                BloxThemeVariant.COLORFUL_DARK,
                listOf(PieceType.I, PieceType.O, PieceType.Z, PieceType.S),
            ),
        )
    }
}
