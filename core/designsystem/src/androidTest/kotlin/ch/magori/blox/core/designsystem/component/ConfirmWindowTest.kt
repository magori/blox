package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
internal class ConfirmWindowTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("the window asks its question as a heading, then offers the safe answer first, each calling its own")
    fun theWindowAsksThenOffersTheSafeAnswerFirst() = extension.use {
        // Given
        val answers = mutableListOf<String>()
        showWindow(onConfirm = { answers += "confirm" }, onDismiss = { answers += "dismiss" })

        // Then the question, then « Garder le record » before « Effacer le record », on the screen as for TalkBack
        onNodeWithText("Effacer le record ?").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val keep = onNode(hasText("Garder le record") and hasClickAction()).getUnclippedBoundsInRoot()
        val clear = onNode(hasText("Effacer le record") and hasClickAction()).getUnclippedBoundsInRoot()
        assertThat(keep.left < clear.left || keep.bottom <= clear.top).isTrue()

        // When
        onNode(hasText("Garder le record") and hasClickAction()).performClick()
        onNode(hasText("Effacer le record") and hasClickAction()).performClick()

        // Then
        runOnIdle { assertThat(answers).containsExactly("dismiss", "confirm") }
    }

    @Test
    @DisplayName("an answer that failed is told in the window, read by TalkBack as it shows, the answers still there")
    fun anAnswerThatFailedIsToldInTheWindow() = extension.use {
        // Given
        showWindow(failed = true)

        // Then
        onNodeWithText("L'opération a échoué.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNode(hasText("Garder le record") and hasClickAction()).assertExists()
        onNode(hasText("Effacer le record") and hasClickAction()).assertExists()
    }

    /** The window that asks whether to clear the record, [failed] once or not. */
    private fun ComposeContext.showWindow(
        failed: Boolean = false,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                ConfirmWindow(
                    title = "Effacer le record ?",
                    dismiss = "Garder le record",
                    confirm = "Effacer le record",
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                    danger = true,
                    failed = failed,
                )
            }
        }
    }
}
