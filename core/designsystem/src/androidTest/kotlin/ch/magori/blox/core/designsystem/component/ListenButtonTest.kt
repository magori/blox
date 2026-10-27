package ch.magori.blox.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
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

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a listen button playing is drawn as the button of its square, active, and not as at rest")
    fun aListenButtonPlayingIsDrawnAsAnActiveButton(kind: BloxThemeKind) = extension.use {
        // Given a listen button playing, beside the button of its square, active, then at rest
        setContent {
            BloxTheme(kind = kind) {
                Column {
                    ListenButton(playing = true, musicName = "Menuet", onClick = {
                    }, modifier = Modifier.testTag(LISTEN))
                    GlyphButton(BloxIcon.STOP, "Actif", onClick = {
                    }, active = true, modifier = Modifier.testTag(ACTIVE))
                    GlyphButton(BloxIcon.STOP, "Repos", onClick = {}, modifier = Modifier.testTag(AT_REST))
                }
            }
        }

        // Then
        val listen = drawn(LISTEN)
        assertThat(listen.sameAs(drawn(ACTIVE))).describedAs("as the active button").isTrue()
        assertThat(listen.sameAs(drawn(AT_REST))).describedAs("as the button at rest").isFalse()
    }

    private fun ComposeContext.drawn(tag: String): Bitmap = onNodeWithTag(tag).captureToImage().asAndroidBitmap()

    private companion object {
        const val LISTEN = "listen"
        const val ACTIVE = "active"
        const val AT_REST = "at_rest"
    }
}
