package ch.magori.blox.core.board

import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.DesignTokens
import ch.magori.blox.core.designsystem.theme.cssColor
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test

/**
 * The colours of the pieces, the `piece-*` tokens of docs/design-system/tokens.json, and the styles of their cells
 * stay the same (ADR 0019): the design system does not know the pieces, their colours are coded here (ADR 0023).
 */
class PieceTokensTest {
    @Test
    fun `each piece token of tokens json is the fill and the highlight of its piece, day and night`() {
        // Given the piece tokens of tokens json: the fill piece-<x> and the highlight piece-<x>-hi of each piece
        val tokens = DesignTokens.colours.filterKeys { it.startsWith("piece-") }

        assertSoftly { softly ->
            // Then tokens json has these two for each piece, and no other piece token
            softly.assertThat(tokens.keys).containsExactlyInAnyOrderElementsOf(
                PieceType.entries.flatMap { listOf(fillToken(it), highlightToken(it)) },
            )
            // And each is the colour of its piece, by day and by night
            for (type in PieceType.entries) {
                val fill = tokens.getValue(fillToken(type))
                val highlight = tokens.getValue(highlightToken(type))
                val written = mapOf(DAY to (fill.day to highlight.day), NIGHT to (fill.night to highlight.night))
                for ((variant, colours) in written) {
                    val style = cellStyle(variant, Cell.Filled(type))
                    softly.assertThat(style.fill).`as`("fill of $type, $variant").isEqualTo(cssColor(colours.first))
                    softly.assertThat(style.bubble?.highlight)
                        .`as`("highlight of $type, $variant")
                        .isEqualTo(cssColor(colours.second))
                }
            }
        }
    }

    private fun fillToken(type: PieceType) = "piece-${type.name.lowercase()}"

    private fun highlightToken(type: PieceType) = "${fillToken(type)}-hi"

    private companion object {
        val DAY = BloxThemeVariant.COLORFUL_LIGHT
        val NIGHT = BloxThemeVariant.COLORFUL_DARK
    }
}
