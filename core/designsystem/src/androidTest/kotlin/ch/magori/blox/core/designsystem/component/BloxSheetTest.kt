package ch.magori.blox.core.designsystem.component

import android.view.KeyEvent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
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
internal class BloxSheetTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a sheet is read as a dialog by TalkBack, its title first")
    fun aSheetIsReadAsADialogItsTitleFirst() = extension.use {
        // Given
        showSheet()

        // Then a dialog, whose first words are its title, a heading, before what it holds
        val dialog = onNode(isDialog()).fetchSemanticsNode()
        assertThat(readOrder(dialog)).startsWith(TITLE, FIRST_CHOICE)
        onNodeWithText(TITLE).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    @DisplayName("the system back and a touch on the scrim close the sheet")
    fun theSystemBackAndATouchOnTheScrimCloseTheSheet() = extension.use {
        // Given
        var closings = 0
        showSheet(onDismissRequest = { closings++ })

        // When the system goes back
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        // Then
        runOnIdle { assertThat(closings).isEqualTo(1) }

        // When the scrim is touched, above the sheet
        onNode(isDialog()).performTouchInput { click(Offset(centerX, SCRIM_TAP.toPx())) }

        // Then
        runOnIdle { assertThat(closings).isEqualTo(2) }
    }

    @Test
    @DisplayName("a sheet lifted by a bottom inset keeps its last row of actions in sight, its content scrolling")
    fun aSheetLiftedByABottomInsetKeepsItsActionsInSight() = extension.use {
        // Given a sheet of many lines and its action, over a keyboard of 320 dp
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                BloxSheet(
                    title = TITLE,
                    onDismissRequest = {},
                    insets = WindowInsets(bottom = KEYBOARD),
                    actions = { WindowButton(text = ACTION, onClick = {}) },
                ) {
                    repeat(LINES) { WindowText("Ligne $it") }
                }
            }
        }

        // Then the action stands whole above the keyboard, and the lines scroll
        val screen = onNode(isDialog()).getUnclippedBoundsInRoot()
        val action = onNode(hasText(ACTION) and hasClickAction()).getUnclippedBoundsInRoot()
        assertThat(action.bottom).isLessThanOrEqualTo(screen.bottom - KEYBOARD)
        assertThat(action.top).isGreaterThanOrEqualTo(screen.top)
        onNode(hasScrollAction() and hasAnyDescendant(hasText("Ligne 0"))).assertExists()
        onNodeWithText("Ligne ${LINES - 1}").assertIsNotDisplayed()
    }

    /** The texts and descriptions under [node], in the order of its tree, the one TalkBack reads them in. */
    private fun readOrder(node: SemanticsNode): List<String> {
        val own = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        return own + node.children.flatMap { readOrder(it) }
    }

    private fun ComposeContext.showSheet(
        onDismissRequest: () -> Unit = {},
        content: @Composable ColumnScope.() -> Unit = { WindowText(FIRST_CHOICE) },
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                BloxSheet(title = TITLE, onDismissRequest = onDismissRequest, content = content)
            }
        }
    }

    private companion object {
        const val TITLE = "Au hasard"
        const val FIRST_CHOICE = "Surprise"

        /** How far from the top of the screen the scrim is touched: far above the sheet. */
        val SCRIM_TAP = 120.dp

        /** A keyboard, high enough to leave little room to the sheet. */
        val KEYBOARD = 320.dp
        const val ACTION = "Tirer une musique"
        const val LINES = 30
    }
}
