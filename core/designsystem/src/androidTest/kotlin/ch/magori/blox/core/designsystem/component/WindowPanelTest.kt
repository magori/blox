package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithText
import ch.magori.blox.core.designsystem.text.textLayout
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxTypographies
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class WindowPanelTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("the title of a window is in screen title, read as a heading")
    fun theTitleOfAWindowIsInScreenTitle() = extension.use {
        // Given
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                WindowPanel(title = TITLE) { WindowText("Elle ne sera plus jouée.") }
            }
        }

        // Then in Unbounded, 22 sp, bold (tokens.json, screen-title)
        val screenTitle = BloxTypographies.Colorful.screenTitle
        val style = textLayout(TITLE).layoutInput.style
        assertThat(listOf(style.fontFamily, style.fontSize, style.fontWeight, style.lineHeight))
            .containsExactly(
                screenTitle.fontFamily,
                screenTitle.fontSize,
                screenTitle.fontWeight,
                screenTitle.lineHeight,
            )
        onNodeWithText(TITLE).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    private companion object {
        const val TITLE = "Supprimer « Thème du soir » ?"
    }
}
