package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class NamingWindowTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("with a problem, the confirmation is disabled and Enter confirms nothing; without, both confirm")
    fun aProblemDisablesTheConfirmationAndEnter() = extension.use {
        // Given a name refused
        var confirmed = 0
        var problem by mutableStateOf<String?>("Une autre musique porte déjà ce nom.")
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                NamingWindow(
                    title = "Renommer « Thème du soir »",
                    name = "Ma marche",
                    onNameChanged = {},
                    maxLength = 40,
                    problem = problem,
                    confirm = "Renommer",
                    onConfirm = { confirmed++ },
                    onDismiss = {},
                )
            }
        }

        // When
        onNode(hasSetTextAction()).performImeAction()

        // Then
        onNodeWithText("Une autre musique porte déjà ce nom.").assertExists()
        onNode(hasClickAction() and hasText("Renommer")).assertIsNotEnabled()
        runOnIdle { assertThat(confirmed).isZero() }

        // When the name is allowed
        problem = null
        onNode(hasSetTextAction()).performImeAction()
        onNode(hasClickAction() and hasText("Renommer")).assertIsEnabled().performClick()

        // Then
        runOnIdle { assertThat(confirmed).isEqualTo(2) }
    }

    @Test
    @DisplayName("a name of 40 characters shows whole with the font twice as large; a line break is a space")
    fun aLongNameShowsWhole() = extension.use {
        // Given a phone of 360 dp, the font twice as large
        val typed = mutableListOf<String>()
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    NamingWindow(
                        title = "Enregistrer la musique",
                        name = LONGEST_NAME,
                        onNameChanged = { typed += it },
                        maxLength = 40,
                        problem = null,
                        confirm = "Enregistrer",
                        onConfirm = {},
                        onDismiss = {},
                        modifier = Modifier.width(360.dp),
                    )
                }
            }
        }

        // Then
        val layouts = mutableListOf<TextLayoutResult>()
        onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val field = onNode(hasSetTextAction()).fetchSemanticsNode()
        assertThat(layouts.first().lineCount).isGreaterThan(1)
        assertThat(field.size.height).isGreaterThanOrEqualTo(layouts.first().size.height)

        // When
        onNode(hasSetTextAction()).performTextReplacement("Ma\nmarche")

        // Then
        runOnIdle { assertThat(typed).containsExactly("Ma marche") }
    }

    private companion object {
        /** A name of 40 characters, the longest a saved name may have. */
        const val LONGEST_NAME = "Marche des géants en fa dièse mixolydien"
    }
}
