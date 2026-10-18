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
    fun `in the Colorful theme titles and values are in Chakra Petch bold, labels in Chakra Petch regular`() {
        // Given the typography of the Colorful theme
        val typography = BloxTypographies.Colorful

        // Then
        assertThat(fontFileOf(typography.title)).isEqualTo("chakra_petch_bold")
        assertThat(fontFileOf(typography.label)).isEqualTo("chakra_petch_regular")
        assertThat(fontFileOf(typography.value)).isEqualTo("chakra_petch_bold")
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
internal fun fontFileOf(style: TextStyle): String {
    val weight = style.fontWeight ?: FontWeight.Normal
    val fonts = (style.fontFamily as FontListFontFamily).fonts
    val font = fonts.single { it.weight == weight } as ResourceFont
    return fontResourceName(font.resId)
}

/** The name of the font resource [resId], which is also its file name in res/font, without the extension. */
internal fun fontResourceName(resId: Int): String = R.font::class.java.fields.single { it.getInt(null) == resId }.name
