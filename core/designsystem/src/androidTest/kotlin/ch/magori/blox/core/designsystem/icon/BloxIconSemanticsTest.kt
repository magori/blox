package ch.magori.blox.core.designsystem.icon

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class BloxIconSemanticsTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("an icon without a description is decorative: TalkBack has nothing to read on it")
    fun anIconWithoutADescriptionIsDecorative(kind: BloxThemeKind) = extension.use {
        // Given
        setContent { BloxTheme(kind = kind) { BloxIcon(BloxIcon.HOME) } }

        // Then no node is described, nor read as an image
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription), useUnmergedTree = true)
            .assertCountEquals(0)
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Role), useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("an icon alone with a description is read by TalkBack as an image of that description")
    fun anIconWithADescriptionIsReadAsAnImage(kind: BloxThemeKind) = extension.use {
        // Given
        setContent { BloxTheme(kind = kind) { BloxIcon(BloxIcon.GEAR, contentDescription = "Paramètres") } }

        // Then
        onNodeWithContentDescription("Paramètres")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
    }
}
