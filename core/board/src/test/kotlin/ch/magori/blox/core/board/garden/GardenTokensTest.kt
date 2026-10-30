package ch.magori.blox.core.board.garden

import androidx.compose.ui.unit.Dp
import ch.magori.blox.core.board.CodedColour
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.DesignTokens
import ch.magori.blox.core.designsystem.theme.assertCodedOrListed
import ch.magori.blox.core.designsystem.theme.cssColor
import ch.magori.blox.core.designsystem.theme.cssDp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The colours and sizes of the cells of the musical garden, tokens `garden-*` of docs/design-system/tokens.json, and
 * those of the code stay the same (ADR 0019). They exist in the Colorful theme only, so they are coded here, with the
 * cells, rather than in the palette, which every theme fills.
 */
class GardenTokensTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("colourTokens")
    fun `each colour token of the garden is its colour in the code, day and night`(token: CodedColour) {
        // Given the token as tokens json writes it
        val written = DesignTokens.colours.getValue(token.name)

        // Then
        assertThat(token.read(BloxThemeVariant.COLORFUL_LIGHT)).isEqualTo(cssColor(written.day))
        assertThat(token.read(BloxThemeVariant.COLORFUL_DARK)).isEqualTo(cssColor(written.night))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sizeTokens")
    fun `each size token of the garden is its size in the code`(token: CodedSize) {
        // Given the token as tokens json writes it
        val written = DesignTokens.values(SIZE).getValue(token.name)

        // Then
        assertThat(token.size).isEqualTo(cssDp(written))
    }

    @Test
    fun `every garden token of tokens json is coded in core board or listed as not coded, with its reason`() {
        // Given the tokens of the garden in tokens json, whatever their group: colours, shadows, radii, sizes and spaces
        val tokens = (
            DesignTokens.colours.keys + DesignTokens.shadows.keys +
                VALUE_GROUPS.flatMap { DesignTokens.values(it).keys }
            ).filter { it.startsWith(GARDEN_TOKEN) }

        // Then each is one of these, compared to the code above, or left out on purpose, and none is missing
        assertCodedOrListed(tokens, colourTokens().map { it.name } + sizeTokens().map { it.name }, NOT_CODED)
    }

    /** A size token of tokens json, and the property of [GardenSizes] that holds it. */
    class CodedSize(val name: String, private val property: String, val size: Dp) {
        override fun toString(): String = "$name → $property"
    }

    companion object {
        /** The prefix of the tokens of the garden in tokens json. */
        private const val GARDEN_TOKEN = "garden-"
        private const val SIZE = "size"

        /** The groups of tokens json written once for both variants. */
        private val VALUE_GROUPS = listOf("radius", SIZE, "spacing")

        /** The tokens of the garden left out of core:board, with the reason why. */
        private val NOT_CODED: Map<String, String> = mapOf(
            "garden-tile-width" to "La largeur des tuiles du bac, mise en page de l'écran du jardin : codée en P10.10.",
        )

        @JvmStatic
        fun colourTokens(): List<CodedColour> = listOf(
            CodedColour("garden-band", "band") { gardenColors(it).band },
            CodedColour("garden-band-cell", "bandCell") { gardenColors(it).bandCell },
            CodedColour("garden-play-head", "playHead") { gardenColors(it).playHead },
            CodedColour("garden-chord-dot", "chordDot") { gardenColors(it).chordDot },
            CodedColour("garden-chord-halo", "chordHalo") { gardenColors(it).chordHalo },
        )

        @JvmStatic
        fun sizeTokens(): List<CodedSize> = listOf(
            CodedSize("garden-cell", "GardenSizes.cell", GardenSizes.cell),
            CodedSize("garden-chord-dot-size", "GardenSizes.chordDot", GardenSizes.chordDot),
            CodedSize("garden-thumb-cell", "GardenSizes.thumbnailCell", GardenSizes.thumbnailCell),
        )
    }
}
