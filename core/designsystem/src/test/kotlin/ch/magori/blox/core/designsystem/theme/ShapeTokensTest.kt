package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import ch.magori.blox.core.designsystem.icon.IconSizes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The radii, sizes and spaces of docs/design-system/tokens.json and those of the code stay the same (ADR 0019): the
 * radii in the shapes of the Colorful theme, the sizes and spaces in the theme of every kind.
 */
class ShapeTokensTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("codedTokens")
    fun `each radius, size and space of tokens json is the one of the code`(token: CodedToken) {
        // Given the token as tokens json writes it
        val written = DesignTokens.values(token.group).getValue(token.name)

        // Then
        assertThat(token.read()).isEqualTo(token.expected(written))
    }

    @Test
    fun `every radius, size and space token of tokens json is in the code or listed as not coded, with its reason`() {
        // Given the radius, size and space tokens of tokens json, but those of the musical garden, `garden-*`: coded
        // with its cells in core:board, and compared to tokens json there (GardenTokensTest)
        val tokens = GROUPS
            .flatMap { group -> DesignTokens.values(group).keys }
            .filterNot { it.startsWith(GARDEN_TOKEN) }

        // Then each is coded or left out on purpose, and nothing is listed that tokens json does not have
        assertCodedOrListed(tokens, CODED.map { it.name }, NOT_CODED)
    }

    /** A token of tokens json, in its [group], and what holds it in the code. */
    class CodedToken(val group: String, val name: String, private val property: String, val read: () -> Any) {
        /** What the code holds for the token [written] so: a shape for a radius, a length for the others. */
        fun expected(written: String): Any = if (group == RADIUS) cornerShape(written) else cssDp(written)

        override fun toString(): String = "$name → $property"
    }

    companion object {
        private const val RADIUS = "radius"
        private val GROUPS = listOf(RADIUS, "size", "spacing")

        /** The prefix of the sizes of the musical garden in tokens json. */
        private const val GARDEN_TOKEN = "garden-"

        private fun radius(
            name: String,
            property: String,
            read: (BloxShapes) -> Shape,
        ) = CodedToken(RADIUS, name, "BloxShapeSets.Colorful.$property") { read(BloxShapeSets.Colorful) }

        private fun size(
            name: String,
            property: String,
            read: () -> Any,
        ) = CodedToken("size", name, property, read)

        private fun space(
            name: String,
            property: String,
            read: () -> Any,
        ) = CodedToken("spacing", name, property, read)

        /** Which property holds which token: the names do not all follow from each other (ADR 0019). */
        private val CODED: List<CodedToken> = listOf(
            radius("radius-cell", "cell") { it.cell },
            radius("radius-md", "md") { it.md },
            radius("radius-snack", "snack") { it.snack },
            radius("radius-window", "window") { it.window },
            radius("radius-card", "card") { it.card },
            radius("radius-pill", "pill") { it.pill },
            size("touch-target", "BloxSizes.minTouchTarget") { BloxSizes.minTouchTarget },
            size("setting-row", "BloxSizes.settingRow") { BloxSizes.settingRow },
            size("setting-row-tablet", "BloxSizes.settingRowTablet") { BloxSizes.settingRowTablet },
            size("switch-track", "BloxSizes.switchTrack") { BloxSizes.switchTrack },
            size("switch-thumb", "BloxSizes.switchThumb") { BloxSizes.switchThumb },
            size("key-arrow", "BloxSizes.keyArrow") { BloxSizes.keyArrow },
            size("key-arrow-compact", "BloxSizes.keyArrowCompact") { BloxSizes.keyArrowCompact },
            size("key-arrow-tablet", "BloxSizes.keyArrowTablet") { BloxSizes.keyArrowTablet },
            size("key-rotate", "BloxSizes.keyRotate") { BloxSizes.keyRotate },
            size("key-rotate-compact", "BloxSizes.keyRotateCompact") { BloxSizes.keyRotateCompact },
            size("key-rotate-tablet", "BloxSizes.keyRotateTablet") { BloxSizes.keyRotateTablet },
            size("sheet-rise", "BloxSizes.sheetRise") { BloxSizes.sheetRise },
            size("message-rise", "BloxSizes.messageRise") { BloxSizes.messageRise },
            size("icon", "IconSizes.alone") { IconSizes.alone },
            space("space-1", "BloxSpaces.space1") { BloxSpaces.space1 },
            space("space-2", "BloxSpaces.space2") { BloxSpaces.space2 },
            space("space-3", "BloxSpaces.space3") { BloxSpaces.space3 },
            space("space-4", "BloxSpaces.space4") { BloxSpaces.space4 },
            space("space-6", "BloxSpaces.space6") { BloxSpaces.space6 },
        )

        /** The radius, size and space tokens left out of the code, with the reason why. */
        private val NOT_CODED: Map<String, String> = mapOf(
            "radius-garden" to "Le cadre de la grille du Jardin musical, dessiné par son écran : codé en P10.10.",
        )

        @JvmStatic
        fun codedTokens(): List<CodedToken> = CODED
    }
}

/** 999 px is how CSS writes a pill: corners of half the height, whatever the height. */
private const val CSS_PILL = "999px"

/** The corners of a radius as tokens json writes it: `16px`, or [CSS_PILL]. */
private fun cornerShape(written: String): Shape =
    if (written == CSS_PILL) RoundedCornerShape(percent = 50) else RoundedCornerShape(cssDp(written))
