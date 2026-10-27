package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * The texts of the screens on their backgrounds, in every theme: the keys of the game, the groups of choices and the
 * switches have their own tests (KeyColorsTest, ChoiceLookTest, SwitchLookTest).
 */
internal class PaletteContrastTest {
    /**
     * A text colour on a background it is drawn on: titles, values and secondary buttons in `ink`, labels of the
     * panels and sections in `label`, on the page or in a panel; the main buttons and the new record badge in
     * `accentInk` on `accent`; a deletion confirmed in `dangerInk` on `danger`, and offered in `danger` in a menu, on
     * `panel`.
     */
    internal enum class TextPair(val text: (BloxPalette) -> Color, val background: (BloxPalette) -> Color) {
        INK_ON_PAGE(BloxPalette::ink, BloxPalette::page),
        LABEL_ON_PAGE(BloxPalette::label, BloxPalette::page),
        INK_ON_PANEL(BloxPalette::ink, BloxPalette::panel),
        LABEL_ON_PANEL(BloxPalette::label, BloxPalette::panel),
        ACCENT_INK_ON_ACCENT(BloxPalette::accentInk, BloxPalette::accent),
        DANGER_INK_ON_DANGER(BloxPalette::dangerInk, BloxPalette::danger),
        DANGER_ON_PANEL(BloxPalette::danger, BloxPalette::panel),
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("texts")
    fun `a text stands out from its background by a contrast of 4,5 to 1 at least`(
        theme: String,
        pair: TextPair,
        palette: BloxPalette,
    ) {
        // When / Then (WCAG 2.1, 1.4.3: text, the small labels of the panels included)
        assertThat(contrast(pair.text(palette), pair.background(palette))).isGreaterThanOrEqualTo(4.5)
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("tabs")
    fun `the label of the open tab of a track stands out from it by 4,5 to 1 at least, day and night`(
        theme: String,
        track: String,
        colors: TrackColors,
    ) {
        // When / Then (WCAG 2.1, 1.4.3): the open track of the editor, and a segment retained in its colours
        // (ChoiceItem.selectedColors), its label and check in tabInk on tab
        assertThat(contrast(colors.tabInk, colors.tab)).isGreaterThanOrEqualTo(4.5)
    }

    companion object {
        @JvmStatic
        fun texts(): List<Arguments> = namedPalettes.flatMap { (theme, palette) ->
            TextPair.entries.map { pair -> Arguments.of(theme, pair, palette) }
        }

        /** The tracks of Couleurs, by day and by night; Original no longer changes. */
        @JvmStatic
        fun tabs(): List<Arguments> = listOf(
            "Couleurs, jour" to BloxPalettes.ColorfulLight,
            "Couleurs, nuit" to BloxPalettes.ColorfulDark,
        ).flatMap { (theme, palette) ->
            listOf(Arguments.of(theme, "mélodie", palette.melody), Arguments.of(theme, "basse", palette.bass))
        }
    }
}
