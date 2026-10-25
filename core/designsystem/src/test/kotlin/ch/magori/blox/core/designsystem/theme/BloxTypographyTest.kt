package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BloxTypographyTest {
    @Test
    fun `in the Original theme titles, labels and values are in the pixel font, without a synthesized bold`() {
        // Given the typography of the Original theme
        val typography = BloxTypographies.Original

        // Then every text is in Press Start 2P, at the only weight it has
        assertThat(fontFileOf(typography.title)).isEqualTo("press_start_2p_regular")
        assertThat(fontFileOf(typography.label)).isEqualTo("press_start_2p_regular")
        assertThat(fontFileOf(typography.value)).isEqualTo("press_start_2p_regular")
    }

    @Test
    fun `in the Original theme each style of the design system is in the pixel font, without a synthesized bold`() {
        // Given the text styles of the Original theme
        val styles = designSystemStyles(BloxTypographies.Original)

        // Then
        assertThat(styles.values.map(::fontFileOf)).containsOnly("press_start_2p_regular")
    }

    @Test
    fun `the logo style is in Bagel Fat One and is used for the logo only`() {
        // Given the typography of the Colorful theme
        val typography = BloxTypographies.Colorful

        // Then the logo is in Bagel Fat One, and no other text is
        assertThat(fontFileOf(typography.logo)).isEqualTo("bagel_fat_one_regular")
        val others = allStyles(typography).filterKeys { it != "logo" }
        assertThat(others.mapValues { (_, style) -> fontFileOf(style) }).doesNotContainValue("bagel_fat_one_regular")
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    fun `each text takes its weight on the weight axis of its font, not the default instance of a variable font`(
        kind: BloxThemeKind,
    ) {
        // Given each text of the theme
        val styles = allStyles(BloxTypographies.of(kind))

        // Then the variable font that draws it sets its weight axis to the weight of the text
        styles.filterValues { embeddedFont(fontFileOf(it)).isVariable }.forEach { (name, style) ->
            val weight = style.fontWeight ?: FontWeight.Normal
            val axis = fontOf(style).variationSettings.settings.single { it.axisName == "wght" }
            assertThat(axis.toVariationValue(null)).withFailMessage { "$name is not drawn at $weight" }
                .isEqualTo(weight.weight.toFloat())
        }
    }

    @Test
    fun `in the Colorful theme titles are in Unbounded bold, labels in Manrope medium and values in Manrope bold`() {
        // Given the typography of the Colorful theme
        val typography = BloxTypographies.Colorful

        // Then
        assertThat(fontFileOf(typography.title)).isEqualTo("unbounded_variable")
        assertThat(typography.title.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(fontFileOf(typography.label)).isEqualTo("manrope_variable")
        assertThat(typography.label.fontWeight).isEqualTo(FontWeight.Medium)
        assertThat(fontFileOf(typography.value)).isEqualTo("manrope_variable")
        assertThat(typography.value.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    fun `the Material styles take the font of the titles for headings and of the labels for the rest`(
        kind: BloxThemeKind,
    ) {
        // Given the typography of the theme
        val typography = BloxTypographies.of(kind)

        // When Material takes it, for the home screen and the buttons
        val material = typography.toMaterial()

        // Then
        with(material) {
            listOf(displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall)
                .plus(listOf(titleLarge, titleMedium, titleSmall))
                .forEach { style -> assertThat(fontFileOf(style)).isEqualTo(fontFileOf(typography.title)) }
            listOf(bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall)
                .forEach { style -> assertThat(fontFileOf(style)).isEqualTo(fontFileOf(typography.label)) }
        }
    }

    @Test
    fun `a text that shrinks to fit its place stays at least 8 sp, to stay readable`() {
        assertThat(BloxTypographies.MinimumFontSize).isEqualTo(8.sp)
    }
}

/**
 * The embedded font file that draws [style]: the font of its family at exactly its weight. A family without
 * that weight would make Android synthesize it, which a pixel font does not survive.
 */
internal fun fontFileOf(style: TextStyle): String = fontResourceName(fontOf(style).resId)

/** The embedded font that draws [style], at exactly its weight. */
private fun fontOf(style: TextStyle): ResourceFont {
    val weight = style.fontWeight ?: FontWeight.Normal
    val fonts = (style.fontFamily as FontListFontFamily).fonts
    return fonts.single { it.weight == weight } as ResourceFont
}

/** The text styles of the design system in [typography], by their name in tokens json. */
private fun designSystemStyles(typography: BloxTypography): Map<String, TextStyle> =
    TypographyTokensTest.codedStyles().associate { it.name to it.read(typography) }

/** Every text of [typography]: its roles, title, label and value, and the styles of the design system. */
private fun allStyles(typography: BloxTypography): Map<String, TextStyle> =
    mapOf("title" to typography.title, "label" to typography.label, "value" to typography.value) +
        designSystemStyles(typography)

/** The name of the font resource [resId], which is also its file name in res/font, without the extension. */
internal fun fontResourceName(resId: Int): String = R.font::class.java.fields.single { it.getInt(null) == resId }.name
