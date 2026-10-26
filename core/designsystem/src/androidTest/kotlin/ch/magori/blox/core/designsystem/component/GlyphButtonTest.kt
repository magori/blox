package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class GlyphButtonTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a button of an icon alone is 48 dp, read as a button of its description, its icon adding nothing")
    fun aButtonOfAnIconAloneIsReadAsAButtonOfItsDescription(kind: BloxThemeKind) = extension.use {
        // Given
        setContent {
            BloxTheme(kind = kind) { GlyphButton(icon = BloxIcon.GEAR, description = "Paramètres", onClick = {}) }
        }

        // Then the button: 48 dp, a button, its description once
        onNodeWithContentDescription("Paramètres")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Paramètres")))
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        onAllNodes(hasContentDescription("Paramètres"), useUnmergedTree = true).assertCountEquals(1)
        // And its icon, there, with nothing for TalkBack
        onNode(hasTestTag(GLYPH_BUTTON_ICON_TAG), useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a bare icon button is 48 dp wide and high at least")
    fun aBareIconButtonIsLargeEnough(kind: BloxThemeKind) = extension.use {
        // Given a disc of nothing but its glyph
        setContent {
            BloxTheme(kind = kind) {
                GlyphButton(description = "Plus d'actions", onClick = {}, emphasis = ButtonEmphasis.BARE) { ink ->
                    Glyphs.more(this, ink)
                }
            }
        }

        // Then
        onNodeWithContentDescription("Plus d'actions")
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
    }
}
