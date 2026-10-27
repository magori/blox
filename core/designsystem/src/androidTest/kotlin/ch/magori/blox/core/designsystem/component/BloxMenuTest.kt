package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class BloxMenuTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a menu item is 48 dp high, read as its label, its icon adding nothing")
    fun aMenuItemIsReadAsItsLabel() = extension.use {
        // Given
        var edited = false
        showMenu { BloxMenuItem(text = "Modifier", icon = BloxIcon.EDIT, onClick = { edited = true }) }

        // Then the item: 48 dp high, read as its label alone
        val item = onNode(hasText("Modifier") and hasClickAction())
        item
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("Modifier"))))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        // And its icon, there, with nothing for TalkBack
        onAllNodes(hasTestTag(MENU_ITEM_ICON_TAG), useUnmergedTree = true).assertCountEquals(1)
        onNode(hasTestTag(MENU_ITEM_ICON_TAG), useUnmergedTree = true)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
        // And touching it does what it says
        item.performClick()
        assertThat(edited).isTrue()
    }

    @Test
    @DisplayName("a checked item is read as a checked check box and bears a check")
    fun aCheckedItemIsReadAsACheckedCheckBox() = extension.use {
        // Given « Suivre la lecture », checked, and « Voir l'autre piste », not
        var follow = true
        showMenu {
            BloxMenuItem(text = "Suivre la lecture", checked = true, onCheckedChange = { follow = it })
            BloxMenuItem(text = "Voir l'autre piste", checked = false, onCheckedChange = {})
        }

        // Then each a check box, 48 dp high, the first checked, the other not
        val checked = onNode(hasText("Suivre la lecture") and hasClickAction())
        checked
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        onNode(hasText("Voir l'autre piste") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
        // And one check only, displayed
        onAllNodes(hasTestTag(MENU_ITEM_CHECK_TAG), useUnmergedTree = true).assertCountEquals(1)
        onNode(hasTestTag(MENU_ITEM_CHECK_TAG), useUnmergedTree = true).assertIsDisplayed()
        // And the check within the item it marks
        val check = onNode(hasTestTag(MENU_ITEM_CHECK_TAG), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertThat(checked.fetchSemanticsNode().boundsInRoot.contains(check.center)).isTrue()
        // And touching it unchecks it
        checked.performClick()
        assertThat(follow).isFalse()
    }

    @Test
    @DisplayName("a touch in the margin around the panel of a menu closes it")
    fun aTouchInTheMarginClosesTheMenu() = extension.use {
        // Given an open menu
        var expanded by mutableStateOf(true)
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box {
                        BloxMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            BloxMenuItem(text = "Modifier", icon = BloxIcon.EDIT, onClick = {})
                        }
                    }
                }
            }
        }

        // When a touch falls 10 dp off the left edge of its panel, in the margin left for its shadow
        onNodeWithTag(MENU_PANEL_TAG).performTouchInput {
            click(Offset(x = -10.dp.toPx(), y = height / 2f))
        }

        // Then the menu is closed
        waitForIdle()
        assertThat(expanded).isFalse()
        onNodeWithTag(MENU_PANEL_TAG).assertDoesNotExist()
    }

    @Test
    @DisplayName("TalkBack announces the menu as it opens, by its title")
    fun theMenuIsAnnouncedAsItOpens() = extension.use {
        // Given
        showMenu { BloxMenuItem(text = "Modifier", icon = BloxIcon.EDIT, onClick = {}) }

        // Then its panel is a pane, titled Menu: TalkBack reads its title when it shows
        onNodeWithTag(MENU_PANEL_TAG).assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Menu"))
    }

    @Test
    @DisplayName("opened from the keyboard, a menu puts the focus on its first item")
    fun openedFromTheKeyboardTheMenuFocusesItsFirstItem() = extension.use {
        // Given the keyboard in use, and the menu not yet open
        var expanded by mutableStateOf(false)
        setContent {
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box {
                    BloxMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BloxMenuItem(text = "Modifier", icon = BloxIcon.EDIT, onClick = {})
                        BloxMenuItem(text = "Supprimer", icon = BloxIcon.TRASH, onClick = {}, danger = true)
                    }
                }
            }
        }
        waitForIdle()

        // When it opens
        expanded = true

        // Then
        onNode(hasText("Modifier") and hasClickAction()).assertIsFocused()
    }

    @Test
    @DisplayName("a menu opened by « … » against the right edge ends at the end of « … », under it")
    fun aMenuAgainstTheRightEdgeEndsAtTheEndOfItsAnchor() = extension.use {
        // Given « … » against the right edge of the screen
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    Box {
                        GlyphButton(description = "Plus d'actions", onClick = {}) { ink -> Glyphs.more(this, ink) }
                        BloxMenu(expanded = true, onDismissRequest = {}) {
                            BloxMenuItem(text = "Modifier", icon = BloxIcon.EDIT, onClick = {})
                        }
                    }
                }
            }
        }

        // Then the panel ends where « … » ends, right under it
        val anchor = onNodeWithContentDescription("Plus d'actions").fetchSemanticsNode()
        val panel = onNodeWithTag(MENU_PANEL_TAG).fetchSemanticsNode()
        val anchorEnd = anchor.positionOnScreen.x + anchor.size.width
        val panelEnd = panel.positionOnScreen.x + panel.size.width
        assertThat(panelEnd).isCloseTo(anchorEnd, within(1f))
        assertThat(panel.positionOnScreen.y).isCloseTo(anchor.positionOnScreen.y + anchor.size.height, within(1f))
    }

    /** A menu open in the Colorful theme, its items [items]. */
    private fun ComposeContext.showMenu(items: @Composable () -> Unit) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box { BloxMenu(expanded = true, onDismissRequest = {}) { items() } }
            }
        }
    }
}
