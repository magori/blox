package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithTag
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
internal class FocusAfterDeletionTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    /** The keys listed, as a test deletes them. */
    private var listed by mutableStateOf(listOf("a", "b", "c"))

    /** The key whose window of deletion is open, if any. */
    private var deleting by mutableStateOf<String?>(null)

    @Test
    @DisplayName("from the keyboard, the element after the one deleted takes the focus, else before it, else the list")
    fun theNextElementTakesTheFocusElseThePreviousElseTheList() = extension.use {
        // Given three elements, the keyboard in use
        showList()

        // When / Then « b » deleted, « c » after it
        delete("b")
        onNodeWithTag("c").assertIsFocused()

        // When / Then « c » deleted, the last one, « a » before it
        delete("c")
        onNodeWithTag("a").assertIsFocused()

        // When / Then « a » deleted, the only one
        delete("a")
        onNodeWithTag(EMPTY).assertIsFocused()
    }

    @Test
    @DisplayName(
        "from the keyboard, the element in the middle out of the list, then its window closed, gives the focus on",
    )
    fun theMiddleElementOutOfTheListThenItsWindowClosedGivesTheFocusOn() = extension.use {
        // Given « a », « b », « c », the window of the deletion of « b » open, the keyboard in use
        showList()
        deleting = "b"
        waitForIdle()

        // When the list is read without « b », the window still open, then the window closes
        listed = listed - "b"
        waitForIdle()
        deleting = null

        // Then
        onNodeWithTag("c").assertIsFocused()
    }

    @Test
    @DisplayName(
        "from the keyboard, the window of the element in the middle closed, then it out of the list, gives the focus on",
    )
    fun theWindowOfTheMiddleElementClosedThenItOutOfTheListGivesTheFocusOn() = extension.use {
        // Given « a », « b », « c », the window of the deletion of « b » open, the keyboard in use
        showList()
        deleting = "b"
        waitForIdle()

        // When the window closes, then the list is read without « b »
        deleting = null
        waitForIdle()
        listed = listed - "b"

        // Then
        onNodeWithTag("c").assertIsFocused()
    }

    @Test
    @DisplayName(
        "from the keyboard, a deletion not confirmed moves the focus nowhere, until the element leaves after all",
    )
    fun aDeletionNotConfirmedMovesTheFocusNowhereUntilTheElementLeaves() = extension.use {
        // Given « a », « b », « c », the window of the deletion of « b » open, the keyboard in use
        showList()
        deleting = "b"
        waitForIdle()

        // When the window closes, « b » still listed
        deleting = null
        waitForIdle()

        // Then
        listOf("a", "c").forEach { onNodeWithTag(it).assertIsNotFocused() }

        // When « b » leaves after all, no other window opened
        listed = listed - "b"

        // Then
        onNodeWithTag("c").assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, after a deletion not confirmed, the focus follows the next deletion confirmed")
    fun afterADeletionNotConfirmedTheFocusFollowsTheNext() = extension.use {
        // Given « a », « b », « c », the deletion of « b » not confirmed, the keyboard in use
        showList()
        deleting = "b"
        waitForIdle()
        deleting = null
        waitForIdle()

        // When « c » is deleted
        delete("c")

        // Then the focus goes to the element before it
        onNodeWithTag("b").assertIsFocused()
    }

    @Test
    @DisplayName("by touch, an element deleted gives the focus to nothing")
    fun byTouchAnElementDeletedGivesTheFocusToNothing() = extension.use {
        // Given « a » alone, by touch
        // Touch mode is the device's: a test before may have left it for the keyboard.
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
        listed = listOf("a")
        showList(keyboard = false)

        // When
        delete("a")

        // Then
        onNodeWithTag(EMPTY).assertIsNotFocused()
    }

    @Test
    @DisplayName("in a lazy list, the element that takes the focus is revealed first, at its index in the list")
    fun inALazyListTheElementThatTakesTheFocusIsRevealedFirst() = extension.use {
        // Given forty elements in a list that shows a few, scrolled to its end, the keyboard in use
        listed = List(40) { "e$it" }
        val revealed = mutableListOf<Int>()
        // The focus of each element, which an element composed later finds again.
        val focus = HashMap<String, FocusRequester>()
        val focusOf = { key: String -> focus.getOrPut(key) { FocusRequester() } }
        setContent {
            Keyboard(true)
            val state = rememberLazyListState(initialFirstVisibleItemIndex = 36)
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                LazyColumn(state = state, modifier = Modifier.height(ELEMENT * 4)) {
                    items(listed, key = { it }) { key ->
                        Element(key, focusOf(key))
                    }
                }
                FocusAfterDeletion(
                    listed = listed,
                    deleting = deleting,
                    windowOpen = deleting != null,
                    focusOf = focusOf,
                    empty = remember { FocusRequester() },
                    reveal = { index ->
                        revealed += index
                        state.scrollToItem(index)
                    },
                )
            }
        }

        // When « e0 » deleted, out of sight
        delete("e0")

        // Then « e1 », at the head of the list, is revealed, then focused
        onNodeWithTag("e1").assertIsFocused()
        runOnIdle { assertThat(revealed).containsExactly(0) }
    }

    /** The window of the deletion of [key] opens, then closes as [key] leaves the list. */
    private fun ComposeContext.delete(key: String) {
        deleting = key
        waitForIdle()
        deleting = null
        listed = listed - key
        waitForIdle()
    }

    /** The [listed] elements in a column, then the empty list, from the [keyboard] or by touch. */
    private fun ComposeContext.showList(keyboard: Boolean = true) {
        setContent {
            Keyboard(keyboard)
            val focus = HashMap<String, FocusRequester>()
            val empty = remember { FocusRequester() }
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Column {
                    listed.forEach { key -> Element(key, remember(key) { FocusRequester() }.also { focus[key] = it }) }
                    if (listed.isEmpty()) {
                        FocusableBlock(Modifier.testTag(EMPTY).focusRequester(empty)) { Text("Vide.") }
                    }
                }
                FocusAfterDeletion(listed, deleting, deleting != null, focusOf = { focus[it] }, empty = empty)
            }
        }
    }

    /** Asks for the keyboard, or for touch. */
    @Composable
    private fun Keyboard(keyboard: Boolean) {
        val inputMode = LocalInputModeManager.current
        LaunchedEffect(Unit) { inputMode.requestInputMode(if (keyboard) InputMode.Keyboard else InputMode.Touch) }
    }

    /** An element of [key], which [focus] focuses. */
    @Composable
    private fun Element(
        key: String,
        focus: FocusRequester,
    ) {
        Box(Modifier.size(ELEMENT).testTag(key).focusRequester(focus).focusable())
    }

    private companion object {
        const val EMPTY = "empty"

        /** The side of an element. */
        val ELEMENT = 48.dp
    }
}
