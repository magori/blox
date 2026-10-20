package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
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
}
