package ch.magori.blox.core.designsystem.theme

import ch.magori.blox.core.designsystem.frame.TICKS_PER_SECOND
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.roundToInt

/**
 * The durations of docs/design-system/tokens.json are written in milliseconds, the game and its screens count in ticks
 * of 1/60 s (ADR 0006): the code holds each one as the whole number of ticks nearest to its milliseconds.
 */
class MotionTokensTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("codedTokens")
    fun `each duration of tokens json is a whole number of ticks, the nearest to its milliseconds`(token: CodedToken) {
        // Given the token as tokens json writes it
        val millis = DesignTokens.values("duration").getValue(token.name).removeSuffix("ms").toInt()

        // Then
        assertThat(token.ticks).isEqualTo((millis * TICKS_PER_SECOND / 1000.0).roundToInt())
    }

    @Test
    fun `every duration token of tokens json is in the code or listed as not coded, with its reason`() {
        // Given the duration tokens of tokens json
        val tokens = DesignTokens.values("duration").keys

        // Then each is coded or left out on purpose, and nothing is listed that tokens json does not have
        assertCodedOrListed(tokens, CODED.map { it.name }, NOT_CODED)
    }

    /** A duration token of tokens json, and the property of [BloxMotion] that holds it, in [ticks]. */
    class CodedToken(val name: String, private val property: String, val ticks: Int) {
        override fun toString(): String = "$name → $property"
    }

    companion object {
        private val CODED: List<CodedToken> = listOf(
            CodedToken("motion-press", "press", BloxMotion.press),
            CodedToken("motion-curtain", "curtain", BloxMotion.curtain),
            CodedToken("motion-card", "card", BloxMotion.card),
            CodedToken("motion-sticker", "sticker", BloxMotion.sticker),
            CodedToken("motion-pop", "pop", BloxMotion.pop),
            CodedToken("motion-reduced-fade", "reducedFade", BloxMotion.reducedFade),
            CodedToken("motion-garden-bounce", "gardenBounce", BloxMotion.gardenBounce),
            CodedToken("motion-garden-glow", "gardenGlow", BloxMotion.gardenGlow),
            CodedToken("motion-garden-fall-step", "gardenFallStep", BloxMotion.gardenFallStep),
        )

        /** The duration tokens left out of the code, with the reason why. */
        private val NOT_CODED: Map<String, String> = emptyMap()

        @JvmStatic
        fun codedTokens(): List<CodedToken> = CODED
    }
}
