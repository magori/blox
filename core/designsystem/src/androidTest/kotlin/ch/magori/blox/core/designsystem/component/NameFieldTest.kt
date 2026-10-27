package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalettes
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
internal class NameFieldTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("the name field counts its characters out of 40, as TalkBack reads it")
    fun theNameFieldCountsItsCharacters() = extension.use {
        // Given a name of 7 characters, a note among them (two units of a string, one character)
        showField(name = "Thème 🎵")

        // Then
        onNodeWithText("7/40").assertContentDescriptionEquals("7 caractères sur 40")

        // When
        onNode(hasSetTextAction()).performTextReplacement("Ma marche")

        // Then
        onNodeWithText("9/40").assertContentDescriptionEquals("9 caractères sur 40")
    }

    @Test
    @DisplayName("a refused name frames the field in danger and says why, read by TalkBack as it shows")
    fun aRefusedNameFramesTheFieldInDangerAndSaysWhy() = extension.use {
        // Given
        showField(name = "Ma marche", problem = TAKEN)

        // Then why, read as it shows, and the field in error
        onNodeWithText(
            TAKEN,
        ).assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, TAKEN))
        // And its frame in danger
        assertThat(frameColour()).isEqualTo(BloxPalettes.ColorfulLight.danger)
    }

    @Test
    @DisplayName("the name field is framed in ink while it has the focus")
    fun theNameFieldIsFramedInInkWhileItHasTheFocus() = extension.use {
        // Given
        showField(name = "Ma marche")

        // When
        onNode(hasSetTextAction()).requestFocus()

        // Then
        assertThat(frameColour()).isEqualTo(BloxPalettes.ColorfulLight.ink)
    }

    /** The colour of the frame of the field: on the left of the box at the bottom of the field, halfway up the box. */
    private fun ComposeContext.frameColour(): Color {
        val pixels = onNode(hasSetTextAction()).captureToImage().toPixelMap()
        val halfABox = with(density) { 28.dp.roundToPx() }
        return pixels[1, pixels.height - halfABox]
    }

    /** The field of a [name], refused for [problem] or not, 320 dp wide, in Couleurs by day. */
    private fun ComposeContext.showField(
        name: String,
        problem: String? = null,
    ) {
        var typed by mutableStateOf(name)
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                NameField(
                    name = typed,
                    onNameChanged = { typed = it },
                    maxLength = 40,
                    problem = problem,
                    onDone = {},
                    modifier = Modifier.width(320.dp),
                )
            }
        }
    }

    private companion object {
        const val TAKEN = "Une autre musique porte déjà ce nom."
    }
}
