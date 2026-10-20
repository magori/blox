package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
internal class WindowTextTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName(
        "in a font twice as large, in Original, on 360 dp, a final point never stands alone on its line, words whole",
    )
    fun aFinalPointNeverStandsAloneOnItsLine() = extension.use {
        // Given a font twice as large, in the pixel font: « enregistrés. » is wider than the window
        showSentence(fontScale = 2f)

        // Then no line holds punctuation only, nor cuts a word, and nothing is clipped
        val layout = textLayout(SENTENCE)
        val lines = List(layout.lineCount) { SENTENCE.substring(layout.getLineStart(it), layout.getLineEnd(it)) }
        assertThat(lines).noneMatch { line -> line.isNotBlank() && line.none(Char::isLetterOrDigit) }
        assertWordsWhole(layout)
        assertThat(layout.hasVisualOverflow).isFalse()
    }

    @Test
    @DisplayName("with the normal font, in Original, on 360 dp, a sentence keeps its size, its words whole")
    fun withTheNormalFontASentenceKeepsItsSize() = extension.use {
        // Given the normal font, in the pixel font
        showSentence(fontScale = 1f)

        // Then it is not shrunk: the 14 sp of bodyMedium, nothing clipped
        val layout = textLayout(SENTENCE)
        assertThat(layout.layoutInput.style.fontSize).isEqualTo(14.sp)
        assertWordsWhole(layout)
        assertThat(layout.hasVisualOverflow).isFalse()
    }

    /** [SENTENCE] in the window that asks before leaving the editor, on 360 dp, the font scaled by [fontScale]. */
    private fun ComposeContext.showSentence(fontScale: Float) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    Box(modifier = Modifier.width(360.dp).padding(horizontal = BloxSizes.windowMargin)) {
                        WindowPanel(title = "Quitter sans enregistrer ?") { WindowText(text = SENTENCE) }
                    }
                }
            }
        }
    }

    private companion object {
        const val SENTENCE = "Les changements de cette musique ne sont pas enregistrés."
    }
}
