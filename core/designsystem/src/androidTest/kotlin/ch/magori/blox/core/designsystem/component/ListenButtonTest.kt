package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ListenButtonTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "a listen button is a button of 48 dp or the size given, read with the name of its music, or its verb alone",
    )
    fun theListenButtonIsReadWithTheNameOfTheMusic(kind: BloxThemeKind) = extension.use {
        // Given
        var listened = 0
        setContent {
            BloxTheme(kind = kind) {
                Column {
                    ListenButton(playing = false, musicName = "Menuet", onClick = { listened++ })
                    ListenButton(playing = true, musicName = "Menuet", onClick = {}, size = 56.dp)
                    ListenButton(playing = false, musicName = null, onClick = {}, enabled = false)
                    ListenButton(playing = true, musicName = null, onClick = {})
                }
            }
        }

        // Then each is a button, read once, its icon adding nothing
        listOf("Écouter Menuet", "Arrêter Menuet", "Écouter", "Arrêter").forEach { description ->
            onNodeWithContentDescription(description)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)))
            onNode(hasTestTag(GLYPH_BUTTON_ICON_TAG) and hasAnyAncestor(hasContentDescription(description)), true)
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        }
        onNodeWithContentDescription("Écouter Menuet").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        onNodeWithContentDescription("Arrêter Menuet").assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp)
        onNodeWithContentDescription("Écouter").assertIsNotEnabled()

        // When
        onNodeWithContentDescription("Écouter Menuet").performClick()

        // Then
        runOnIdle { assertThat(listened).isEqualTo(1) }
    }
}
