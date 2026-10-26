package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** The colours of docs/design-system/tokens.json and the palettes of the Colorful theme stay the same (ADR 0019). */
class PaletteTokensTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("codedTokens")
    fun `each colour token of tokens json has its value in the palette of the day and in the palette of the night`(
        token: CodedToken,
    ) {
        // Given the token as tokens json writes it
        val written = DesignTokens.colours.getValue(token.name)

        // Then
        assertThat(token.read(BloxPalettes.ColorfulLight)).isEqualTo(cssColor(written.day))
        assertThat(token.read(BloxPalettes.ColorfulDark)).isEqualTo(cssColor(written.night))
    }

    @Test
    fun `every colour token of tokens json is in the palette, in the pieces or listed as not coded, with its reason`() {
        // Given the colour tokens of tokens json, but the colours of the pieces, `piece-*`: coded with the styles of the
        // cells in core:board, which knows the pieces (ADR 0023), and compared to tokens json there (PieceTokensTest)
        val tokens = DesignTokens.colours.keys.filterNot { it.startsWith(PIECE_TOKEN) }

        // Then each is coded or left out on purpose, and nothing is listed that tokens json does not have
        assertThat(tokens).containsExactlyInAnyOrderElementsOf(CODED.map { it.name }.toSet() + NOT_CODED.keys)
        assertThat(NOT_CODED.keys).doesNotContainAnyElementsOf(CODED.map { it.name })
        assertThat(NOT_CODED.values).allSatisfy { reason -> assertThat(reason).isNotBlank() }
    }

    @Test
    fun `a colour written rgba in tokens json is read with its alpha`() {
        // Given the scrim of the night, as tokens json writes it
        val written = "rgba(7,8,13,0.8)"

        // When
        val color = cssColor(written)

        // Then
        assertThat(color).isEqualTo(Color(red = 7, green = 8, blue = 13, alpha = 204))
    }

    /** A colour token of tokens json, and the property of [BloxPalette] that holds it. */
    class CodedToken(val name: String, private val property: String, val read: (BloxPalette) -> Color) {
        override fun toString(): String = "$name → $property"
    }

    companion object {
        /**
         * Which property holds which token (ADR 0019): the names do not all follow from each other. A token can be
         * held by several properties, as `melody`, the notes and the open tab of the melody.
         */
        private val CODED: List<CodedToken> = listOf(
            CodedToken("page", "page") { it.page },
            CodedToken("board", "boardBackground") { it.boardBackground },
            CodedToken("empty-cell", "emptyCell") { it.emptyCell },
            CodedToken("panel", "panel") { it.panel },
            CodedToken("panel-border", "panelBorder") { it.panelBorder },
            CodedToken("panel-inset", "panelInset") { it.panelInset },
            CodedToken("ink", "ink") { it.ink },
            CodedToken("label", "label") { it.label },
            CodedToken("label-off", "outOfScale") { it.outOfScale },
            CodedToken("button", "button") { it.button },
            CodedToken("button-ink", "buttonInk") { it.buttonInk },
            CodedToken("button-outline", "buttonOutline") { it.buttonOutline },
            CodedToken("button-pressed", "buttonPressed") { it.buttonPressed },
            CodedToken("accent", "accent") { it.accent },
            CodedToken("accent-ink", "accentInk") { it.accentInk },
            CodedToken("accent-pressed", "accentPressed") { it.accentPressed },
            CodedToken("accent-ring", "accentRing") { it.accentRing },
            CodedToken("danger", "danger") { it.danger },
            CodedToken("danger-ink", "dangerInk") { it.dangerInk },
            CodedToken("danger-soft", "dangerSoft") { it.dangerSoft },
            CodedToken("scrim", "scrim") { it.scrim },
            CodedToken("clear-glow", "clearGlow") { it.clearGlow },
            CodedToken("cell-dead", "cellDead") { it.cellDead },
            CodedToken("cell-ground", "cellGround") { it.cellGround },
            CodedToken("grid-line", "gridLine") { it.gridLine },
            CodedToken("melody", "melody.note") { it.melody.note },
            CodedToken("melody", "melody.tab") { it.melody.tab },
            CodedToken("melody-soft", "melody.held") { it.melody.held },
            CodedToken("bass", "bass.note") { it.bass.note },
            CodedToken("bass", "bass.tab") { it.bass.tab },
            CodedToken("bass-soft", "bass.held") { it.bass.held },
            CodedToken("track-ink", "melody.tabInk") { it.melody.tabInk },
            CodedToken("track-ink", "bass.tabInk") { it.bass.tabInk },
            CodedToken("iris-1", "iris.start") { it.iris.start },
            CodedToken("iris-2", "iris.middle") { it.iris.middle },
            CodedToken("iris-3", "iris.end") { it.iris.end },
        )

        /** The prefix of the colours of the pieces in tokens json. */
        private const val PIECE_TOKEN = "piece-"

        /** The colour tokens left out of [BloxPalette], with the reason why. */
        private val NOT_CODED: Map<String, String> = emptyMap()

        @JvmStatic
        fun codedTokens(): List<CodedToken> = CODED
    }
}
