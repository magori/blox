package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import ch.magori.blox.core.designsystem.icon.BloxIcon
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

    /** A menu open in the Colorful theme, its items [items]. */
    private fun ComposeContext.showMenu(items: @Composable () -> Unit) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box { BloxMenu(expanded = true, onDismissRequest = {}) { items() } }
            }
        }
    }
}
