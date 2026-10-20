package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.text.textLayout
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class WindowAnswersTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("in a normal font, on 360 dp, the answers of a window stand side by side, the safe one first")
    fun inANormalFontTheAnswersStandSideBySide() = extension.use {
        // Given a normal font, in the pixel font
        showWindow(fontScale = 1f, kind = BloxThemeKind.ORIGINAL) { SaveAnswers() }

        // Then each 48 dp high at least, each word whole
        val cancel = answer("Annuler")
        val save = answer("Enregistrer")
        assertThat(cancel.right).isLessThanOrEqualTo(save.left)
        assertThat(cancel.top).isEqualTo(save.top)
        assertAtLeastATouchTargetHigh(cancel, save)
        assertWordsWhole("Annuler", "Enregistrer")
    }

    @Test
    @DisplayName("in a normal font, in Original, on 320 dp, the answers of a window stand one above the other")
    fun inANormalFontOnANarrowPhoneTheAnswersStandOneAboveTheOther() = extension.use {
        // Given a normal font, in the pixel font, on the narrowest phones: « Enregistrer » is wider than half of it
        showWindow(fontScale = 1f, kind = BloxThemeKind.ORIGINAL, width = 320.dp) { SaveAnswers() }

        // Then they stand one above the other, as with a large font, rather than cut « Enregistrer »
        val cancel = answer("Annuler")
        val save = answer("Enregistrer")
        assertThat(cancel.bottom).isLessThanOrEqualTo(save.top)
        assertWordsWhole("Annuler", "Enregistrer")
    }

    @Test
    @DisplayName(
        "in a font twice as large, in Original, on 360 dp, answers too wide side by side stand one above the other",
    )
    fun inALargeFontTheAnswersTooWideStandOneAboveTheOther() = extension.use {
        // Given a font twice as large, in the pixel font: « Annuler » does not hold whole in half the window
        showWindow(fontScale = 2f, kind = BloxThemeKind.ORIGINAL) { SaveAnswers() }

        // Then the safe one first, each as wide as the window, each word whole
        val cancel = answer("Annuler")
        val save = answer("Enregistrer")
        assertThat(cancel.bottom).isLessThanOrEqualTo(save.top)
        assertThat(cancel.width).isEqualTo(save.width)
        assertAtLeastATouchTargetHigh(cancel, save)
        assertWordsWhole("Annuler", "Enregistrer")
    }

    @Test
    @DisplayName("in a font twice as large, in Colorful, on 360 dp, answers whose words hold whole stay side by side")
    fun inALargeFontAnswersWhoseWordsHoldStaySideBySide() = extension.use {
        // Given a font twice as large, in Chakra Petch: « Garder la musique » goes to the line between its words
        showWindow(fontScale = 2f, kind = BloxThemeKind.COLORFUL) {
            WindowAnswers(dismiss = "Garder la musique", confirm = "Supprimer", onConfirm = {}, onDismiss = {})
        }

        // Then
        val keep = answer("Garder la musique")
        val delete = answer("Supprimer")
        assertThat(keep.right).isLessThanOrEqualTo(delete.left)
        assertAtLeastATouchTargetHigh(keep, delete)
        assertWordsWhole("Garder la musique", "Supprimer")
    }

    @Test
    @DisplayName(
        "in a font twice as large, a word too long for the whole width of an answer shrinks rather than be cut",
    )
    fun inALargeFontAWordTooLongShrinksRatherThanBeCut() = extension.use {
        // Given a font twice as large, in the pixel font, an answer whose word is wider than the window
        showWindow(fontScale = 2f, kind = BloxThemeKind.ORIGINAL) {
            WindowButton(text = "Réinitialisation", onClick = {})
        }

        // Then
        assertWordsWhole("Réinitialisation")
        assertThat(textLayout("Réinitialisation").hasVisualOverflow).isFalse()
    }

    /** « Annuler » and « Enregistrer », the answers of the window that saves a music. */
    @Composable
    private fun SaveAnswers() {
        WindowAnswers(confirm = "Enregistrer", onConfirm = {}, onDismiss = {})
    }

    /** [content] in a window on a phone [width] wide, the font scaled by [fontScale], in the theme [kind]. */
    private fun ComposeContext.showWindow(
        fontScale: Float,
        kind: BloxThemeKind,
        width: Dp = 360.dp,
        content: @Composable () -> Unit,
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    Box(modifier = Modifier.width(width).padding(horizontal = BloxSizes.windowMargin)) {
                        WindowPanel(title = "Enregistrer la musique") { content() }
                    }
                }
            }
        }
    }

    /** Where the answer labelled [text] stands. */
    private fun ComposeContext.answer(text: String): Rect =
        onNode(hasText(text) and hasClickAction()).fetchSemanticsNode().boundsInRoot

    /** Each of [answers] is 48 dp high at least. */
    private fun ComposeContext.assertAtLeastATouchTargetHigh(vararg answers: Rect) {
        val target = with(density) { BloxSizes.minTouchTarget.toPx() }
        answers.forEach { assertThat(it.height).isGreaterThanOrEqualTo(target) }
    }

    /** Each line of the labels [texts] holds whole words only. */
    private fun ComposeContext.assertWordsWhole(vararg texts: String) {
        texts.forEach { assertWordsWhole(textLayout(it)) }
    }
}
