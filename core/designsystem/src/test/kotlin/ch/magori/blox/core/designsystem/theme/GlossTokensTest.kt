package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

/** The shadows of docs/design-system/tokens.json and the glosses of the Colorful theme stay the same (ADR 0019). */
class GlossTokensTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("codedTokens")
    fun `each shadow of tokens json is the gloss of the code, day and night`(token: CodedToken) {
        // Given the token as tokens json writes it
        val written = DesignTokens.shadows.getValue(token.name)

        // Then
        assertThat(token.read(BloxGlossSets.ColorfulLight)).isEqualTo(cssGloss(written.day))
        assertThat(token.read(BloxGlossSets.ColorfulDark)).isEqualTo(cssGloss(written.night))
    }

    @Test
    fun `every shadow token of tokens json is a gloss of the code`() {
        // Given the shadow tokens of tokens json
        val tokens = DesignTokens.shadows.keys

        // Then
        assertThat(tokens).containsExactlyInAnyOrderElementsOf(CODED.map { it.name })
    }

    @Test
    fun `a shadow written in tokens json is read as its inner shadows and its ring`() {
        // Given the cell gloss of the day, as tokens json writes it
        val written =
            "inset 0 1px 0 rgba(255,255,255,0.9), inset 0 -2px 0 rgba(28,27,46,0.16), 0 0 0 1px rgba(28,27,46,0.14)"

        // When
        val gloss = cssGloss(written)

        // Then a light band at the top, a shade at the bottom, and a ring around
        assertThat(gloss.layers).containsExactly(
            GlossLayer.Inner(offsetY = 1.dp, color = Color.White.copy(alpha = 0.9f)),
            GlossLayer.Inner(offsetY = (-2).dp, color = Color(red = 28, green = 27, blue = 46).copy(alpha = 0.16f)),
            GlossLayer.Ring(width = 1.dp, color = Color(red = 28, green = 27, blue = 46).copy(alpha = 0.14f)),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["0 4px 8px rgba(28,27,46,0.2)", "inset 2px 1px 0 #FFFFFF", "inset 0 1px 2px #FFFFFF"])
    fun `a shadow blurred or moved sideways is refused, the code could not draw it`(written: String) {
        // Then
        assertThatThrownBy { cssGloss(written) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a ring of a negative width is refused`() {
        // Then
        assertThatThrownBy { cssGloss("0 0 0 -1px #1C1B2E") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    /** A shadow token of tokens json, and the property of [BloxGlosses] that holds it. */
    class CodedToken(val name: String, private val property: String, val read: (BloxGlosses) -> Gloss) {
        override fun toString(): String = "$name → $property"
    }

    companion object {
        private val CODED: List<CodedToken> = listOf(
            CodedToken("button-gloss", "button") { it.button },
            CodedToken("button-gloss-pressed", "buttonPressed") { it.buttonPressed },
            CodedToken("cell-gloss", "cell") { it.cell },
        )

        @JvmStatic
        fun codedTokens(): List<CodedToken> = CODED
    }
}
