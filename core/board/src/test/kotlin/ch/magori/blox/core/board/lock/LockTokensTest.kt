package ch.magori.blox.core.board.lock

import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.DesignTokens
import ch.magori.blox.core.designsystem.theme.cssColor
import ch.magori.blox.core.designsystem.theme.cssGloss
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The colours of the padlock of the Locksmith, tokens of docs/design-system/tokens.json, and the colours of its cells
 * stay the same (ADR 0019). They exist in the Colorful theme only, so they are coded here, with the cells, rather than
 * in the palette, which every theme fills.
 */
class LockTokensTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("colourTokens")
    fun `each colour token of the padlock is its colour in the code, day and night`(token: CodedToken) {
        // Given the token as tokens json writes it
        val written = DesignTokens.colours.getValue(token.name)

        // Then
        assertThat(token.read(BloxThemeVariant.COLORFUL_LIGHT)).isEqualTo(cssColor(written.day))
        assertThat(token.read(BloxThemeVariant.COLORFUL_DARK)).isEqualTo(cssColor(written.night))
    }

    @Test
    fun `the hole inset of tokens json is the inner shadow of a hole, day and night`() {
        // Given the token as tokens json writes it
        val written = DesignTokens.shadows.getValue(HOLE_INSET)

        // Then
        assertThat(lockColors(BloxThemeVariant.COLORFUL_LIGHT).holeInset).isEqualTo(cssGloss(written.day))
        assertThat(lockColors(BloxThemeVariant.COLORFUL_DARK).holeInset).isEqualTo(cssGloss(written.night))
    }

    @Test
    fun `every lock token of tokens json is coded in core board`() {
        // Given the tokens of the padlock in tokens json, colours and shadows
        val tokens = (DesignTokens.colours.keys + DesignTokens.shadows.keys).filter { it.startsWith(LOCK_TOKEN) }

        // Then each is one of these, compared to the code above, and none is missing
        assertThat(tokens).containsExactlyInAnyOrderElementsOf(colourTokens().map { it.name } + HOLE_INSET)
    }

    /** A colour token of tokens json, and the property of [LockColors] that holds it in a variant. */
    class CodedToken(val name: String, private val property: String, val read: (BloxThemeVariant) -> Color) {
        override fun toString(): String = "$name → $property"
    }

    companion object {
        /** The prefix of the tokens of the padlock in tokens json. */
        private const val LOCK_TOKEN = "lock-"
        private const val HOLE_INSET = "lock-hole-inset"

        @JvmStatic
        fun colourTokens(): List<CodedToken> = listOf(
            CodedToken("lock-metal", "metal") { lockColors(it).metal },
            CodedToken("lock-rivet", "rivet") { lockColors(it).rivet },
            CodedToken("lock-hint", "hintFill") { lockColors(it).hintFill },
        )
    }
}
