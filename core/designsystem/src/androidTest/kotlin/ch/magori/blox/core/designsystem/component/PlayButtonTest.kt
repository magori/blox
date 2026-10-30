package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class PlayButtonTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("Jouer is read as a button of its description, its triangle adding nothing")
    fun jouerIsReadAsAButtonOfItsDescription(kind: BloxThemeKind) = extension.use {
        // Given
        var played = 0
        showPlayButton(REMINDER, kind = kind, onClick = { played++ })

        // Then
        onNodeWithContentDescription(DESCRIPTION)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(DESCRIPTION)))
        onNode(
            hasTestTag(PLAY_BUTTON_ICON_TAG) and hasAnyAncestor(hasContentDescription(DESCRIPTION)),
            useUnmergedTree = true,
        )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))

        // When
        onNodeWithContentDescription(DESCRIPTION).performClick()

        // Then
        runOnIdle { assertThat(played).isEqualTo(1) }
    }

    @Test
    @DisplayName("a reminder too long for Jouer is cut by an ellipsis on one line, and TalkBack still reads it whole")
    fun aReminderTooLongIsCutButReadWhole() = extension.use {
        // Given a reminder of the longest name of a music, on 360 dp (moved from HomeScreenTest)
        val reminder = "Classique · Niveau 05 · Ballades pour une longue soirée en hiver"
        showPlayButton(reminder)

        // Then the reminder holds one line, cut
        val layout = textLayout(reminder)
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.isLineEllipsized(0)).isTrue()
        // And TalkBack reads the description whole
        onNodeWithContentDescription(DESCRIPTION).assertExists()
    }

    private fun ComposeContext.showPlayButton(
        reminder: String,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        onClick: () -> Unit = {},
    ) {
        setContent {
            BloxTheme(kind = kind) {
                PlayButton(
                    label = "Jouer",
                    reminder = reminder,
                    description = DESCRIPTION,
                    onClick = onClick,
                    modifier = Modifier.width(NARROW_PHONE),
                )
            }
        }
    }

    /** The text [text], as laid out: the layout of its own node, not merged into the button. */
    private fun ComposeContext.textLayout(text: String): TextLayoutResult {
        val node = onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }

    private companion object {
        const val REMINDER = "Classique · Niveau 05 · Menuet"
        const val DESCRIPTION = "Jouer, Classique, niveau 5, Menuet"
        val NARROW_PHONE = 360.dp
    }
}
