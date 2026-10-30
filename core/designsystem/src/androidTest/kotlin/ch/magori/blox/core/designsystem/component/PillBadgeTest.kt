package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class PillBadgeTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a badge shows its text as given, not in capitals")
    fun aBadgeShowsItsTextAsGiven(kind: BloxThemeKind) = extension.use {
        // Given
        setContent { BloxTheme(kind = kind) { PillBadge(text = "ouvert") } }

        // Then
        onNodeWithText("ouvert").assertExists()
        onNodeWithText("OUVERT").assertDoesNotExist()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large and a narrow place, a badge stays on one line unless told otherwise")
    fun aBadgeStaysOnOneLine(kind: BloxThemeKind) = extension.use {
        // Given a place too narrow for its text on one line
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = kind) {
                    Column {
                        Box(Modifier.width(80.dp)) { PillBadge(text = "Nouveau meilleur") }
                        Box(Modifier.width(80.dp)) { PillBadge(text = "Nouveau record", maxLines = Int.MAX_VALUE) }
                    }
                }
            }
        }

        // Then by default one line; with more lines allowed, it breaks
        assertThat(onNodeWithText("Nouveau meilleur").textLayout().lineCount).isEqualTo(1)
        assertThat(onNodeWithText("Nouveau record").textLayout().lineCount).isGreaterThan(1)
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a badge bears a check before its text only with withCheck")
    fun aBadgeBearsACheckOnlyWithWithCheck(kind: BloxThemeKind) = extension.use {
        // Given the same badge, with and without its check
        setContent {
            BloxTheme(kind = kind) {
                Column {
                    PillBadge(text = "ouvert", modifier = Modifier.testTag(PLAIN))
                    PillBadge(text = "ouvert", modifier = Modifier.testTag(CHECKED), withCheck = true)
                }
            }
        }

        // Then the check, 12 dp, and its gap, 4 dp, widen it; its text starts after them
        val plain = onNodeWithTag(PLAIN).getUnclippedBoundsInRoot()
        val checked = onNodeWithTag(CHECKED).getUnclippedBoundsInRoot()
        assertThat(((checked.right - checked.left) - (plain.right - plain.left)).value).isCloseTo(16f, offset(0.5f))
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("the modifier of a badge applies to its root, its pill around its text")
    fun theModifierOfABadgeAppliesToItsRoot(kind: BloxThemeKind) = extension.use {
        // Given a badge tagged and described by its screen
        setContent {
            BloxTheme(kind = kind) {
                PillBadge(
                    text = "En jeu",
                    modifier = Modifier.testTag(PLAIN).semantics { contentDescription = "Musique en jeu" },
                )
            }
        }

        // Then the tag and the description are on the pill, which holds the text and the room around it
        onNodeWithTag(PLAIN).assertContentDescriptionEquals("Musique en jeu")
        onNode(hasText("En jeu") and hasAnyAncestor(hasTestTag(PLAIN)), useUnmergedTree = true).assertExists()
        val pill = onNodeWithTag(PLAIN).getUnclippedBoundsInRoot()
        val text = onNode(hasText("En jeu"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(((pill.right - pill.left) - (text.right - text.left)).value).isCloseTo(16f, offset(0.5f))
    }

    private companion object {
        const val PLAIN = "badge"
        const val CHECKED = "badge_checked"
    }
}

private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val layouts = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
    return layouts.single()
}
