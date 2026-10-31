package ch.magori.blox.core.designsystem.component

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class RowMenuTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("« … » is read « Plus d'actions pour X » and offers its items in order, Renommer and Supprimer")
    fun theMenuOffersItsItemsInOrder() = extension.use {
        // Given the menu of « Pluie douce », an item of its own before Renommer and Supprimer
        val calls = mutableListOf<String>()
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box {
                    RowMenu(description = rowMenuDescription("Pluie douce"), windowOpen = false) {
                        Item(text = "Modifier", icon = BloxIcon.EDIT, onClick = { calls += "edit" })
                        RenameItem(onRename = { calls += "rename" })
                        DeleteItem(onDelete = { calls += "delete" })
                    }
                }
            }
        }

        // When
        onNodeWithContentDescription(MENU).performClick()

        // Then its items, one under the other, in the order given
        val tops = listOf("Modifier", "Renommer", "Supprimer").map { item ->
            onNode(hasText(item) and hasClickAction()).fetchSemanticsNode().boundsInRoot.top
        }
        assertThat(tops).isSorted().doesNotHaveDuplicates()

        // When each is touched in turn, the menu opened again before each
        onNode(hasText("Renommer") and hasClickAction()).performClick()
        onNodeWithTag(MENU_PANEL_TAG).assertDoesNotExist()
        onNodeWithContentDescription(MENU).performClick()
        onNode(hasText("Supprimer") and hasClickAction()).performClick()
        onNodeWithContentDescription(MENU).performClick()
        onNode(hasText("Modifier") and hasClickAction()).performClick()

        // Then each did what it says, and closed the menu
        runOnIdle { assertThat(calls).containsExactly("rename", "delete", "edit") }
        onNodeWithTag(MENU_PANEL_TAG).assertDoesNotExist()
    }

    @Test
    @DisplayName("a menu read by the description given offers an item to check, which closes the menu once touched")
    fun aMenuOffersAnItemToCheck() = extension.use {
        // Given the menu of the bar of the editor, « Suivre la lecture » checked
        var following by mutableStateOf(true)
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box {
                    RowMenu(description = "Plus d'options", windowOpen = false) {
                        CheckItem(text = "Suivre la lecture", checked = following, onCheckedChange = { following = it })
                    }
                }
            }
        }

        // When
        onNodeWithContentDescription("Plus d'options").performClick()

        // Then
        onNode(hasText("Suivre la lecture") and hasClickAction()).assertIsOn()

        // When
        onNode(hasText("Suivre la lecture") and hasClickAction()).performClick()

        // Then
        runOnIdle { assertThat(following).isFalse() }
        onNodeWithTag(MENU_PANEL_TAG).assertDoesNotExist()
    }

    @Test
    @DisplayName("from the keyboard, « … » waits for the window an item opened to close, then takes the focus back")
    fun theFocusGoesBackOnceTheWindowCloses() = extension.use {
        // Given the keyboard in use, and the menu of « Pluie douce »
        var windowOpen by mutableStateOf(false)
        setContent { RowAndWindow(windowOpen = windowOpen, onRename = { windowOpen = true }) }
        onNodeWithContentDescription(MENU).performSemanticsAction(SemanticsActions.OnClick)

        // When Renommer opens its window
        onNode(hasText("Renommer") and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)

        // Then the window has the focus, not « … »
        onNodeWithTag(WINDOW_TAG).assertIsFocused()
        onNodeWithContentDescription(MENU).assertIsNotFocused()

        // When the window closes
        windowOpen = false

        // Then
        onNodeWithContentDescription(MENU).assertIsFocused()
    }

    @Test
    @DisplayName("a window that opens frames after its item still gives the focus back to « … » as it closes")
    fun aWindowOpenedAFrameLaterGivesTheFocusBack() = extension.use {
        // Given the keyboard in use, and a window opened frames after Renommer, as a view model would
        var asked by mutableStateOf(false)
        var windowOpen by mutableStateOf(false)
        setContent {
            LaunchedEffect(asked) {
                if (asked) {
                    repeat(FRAMES) { withFrameNanos {} }
                    windowOpen = true
                }
            }
            RowAndWindow(windowOpen = windowOpen, onRename = { asked = true })
        }
        onNodeWithContentDescription(MENU).performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasText("Renommer") and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithTag(WINDOW_TAG).assertIsFocused()

        // When the window closes
        windowOpen = false

        // Then
        onNodeWithContentDescription(MENU).assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, the menu closed by Back gives the focus back to « … »")
    fun theMenuClosedByBackGivesTheFocusBack() = extension.use {
        // Given the keyboard in use, and the menu of « Pluie douce » open
        setContent { RowAndWindow(windowOpen = false, onRename = {}) }
        onNodeWithContentDescription(MENU).performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithTag(MENU_PANEL_TAG).assertExists()

        // When
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        // Then
        onNodeWithTag(MENU_PANEL_TAG).assertDoesNotExist()
        onNodeWithContentDescription(MENU).assertIsFocused()
    }

    /**
     * The keyboard in use, the menu of « Pluie douce » offering Renommer ([onRename]) and, while [windowOpen], a window
     * that takes the focus; above « … », something else focusable.
     */
    @Composable
    private fun RowAndWindow(
        windowOpen: Boolean,
        onRename: () -> Unit,
    ) {
        val inputMode = LocalInputModeManager.current
        LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
        BloxTheme(kind = BloxThemeKind.COLORFUL) {
            Column {
                // Focusable before « … »: a focus lost would land on it, not on « … ».
                Box(Modifier.size(48.dp).focusable())
                Box {
                    RowMenu(description = rowMenuDescription("Pluie douce"), windowOpen = windowOpen) {
                        RenameItem(onRename = onRename)
                    }
                }
                if (windowOpen) {
                    val focus = remember { FocusRequester() }
                    Box(Modifier.size(48.dp).testTag(WINDOW_TAG).focusRequester(focus).focusable())
                    LaunchedEffect(Unit) { focus.requestFocus() }
                }
            }
        }
    }

    private companion object {
        const val MENU = "Plus d'actions pour Pluie douce"
        const val WINDOW_TAG = "window"

        /** The frames a view model may take to open the window. */
        const val FRAMES = 3
    }
}
