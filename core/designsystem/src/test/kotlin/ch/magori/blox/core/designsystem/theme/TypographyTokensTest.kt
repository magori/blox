package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** The text styles of docs/design-system/tokens.json and the typography of the Colorful theme stay the same (ADR 0019). */
class TypographyTokensTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("codedStyles")
    fun `each text style of tokens json has its family, size, line height and weight in the Colorful typography`(
        token: CodedStyle,
    ) {
        // Given the style as tokens json writes it
        val written = DesignTokens.textStyles.getValue(token.name)

        // When the Colorful typography gives it
        val style = token.read(BloxTypographies.Colorful)

        // Then its font file is the one of its family (Bagel Fat One is bagel_fat_one_regular.ttf)
        assertThat(fontFileOf(style).substringBeforeLast('_')).isEqualTo(written.family.lowercase().replace(' ', '_'))
        assertThat(style.fontSize).isEqualTo(written.fontSize.sp)
        assertThat(style.lineHeight).isEqualTo(written.lineHeight.em)
        assertThat(style.fontWeight).isEqualTo(FontWeight(written.fontWeight))
    }

    @Test
    fun `every text style of tokens json is in the Colorful typography, and nothing more`() {
        // Given the text styles of tokens json
        val styles = DesignTokens.textStyles.keys

        // Then each is coded, once
        assertThat(CODED.map { it.name }).containsExactlyInAnyOrderElementsOf(styles)
    }

    /** A text style of tokens json, and the property of [BloxTypography] that holds it. */
    class CodedStyle(
        val name: String,
        private val property: String,
        /** Whether the style lays numbers out, with digits of one width. */
        val digits: Boolean = false,
        val read: (BloxTypography) -> TextStyle,
    ) {
        override fun toString(): String = "$name → $property"
    }

    companion object {
        /**
         * Which property holds which style (ADR 0019). The tablet sizes that the usage of some styles gives (28 px for
         * screen-title) are not tokens: the layouts of the tablet take them.
         */
        private val CODED: List<CodedStyle> = listOf(
            CodedStyle("screen-title", "screenTitle") { it.screenTitle },
            CodedStyle("final-score", "finalScore", digits = true) { it.finalScore },
            CodedStyle("record", "record", digits = true) { it.record },
            CodedStyle("stat-value", "statValue", digits = true) { it.statValue },
            CodedStyle("game-title", "gameTitle") { it.gameTitle },
            CodedStyle("stat-label", "statLabel") { it.statLabel },
            CodedStyle("button-label", "buttonLabel") { it.buttonLabel },
            CodedStyle("choice", "choice") { it.choice },
            CodedStyle("section", "section") { it.section },
            CodedStyle("small", "small") { it.small },
            CodedStyle("play-label", "playLabel") { it.playLabel },
            CodedStyle("four-lines-pop", "fourLinesPop") { it.fourLinesPop },
            CodedStyle("points-pop", "pointsPop") { it.pointsPop },
            CodedStyle("button-label-strong", "buttonLabelStrong") { it.buttonLabelStrong },
            CodedStyle("logo", "logo") { it.logo },
        )

        @JvmStatic
        fun codedStyles(): List<CodedStyle> = CODED
    }
}
