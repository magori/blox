package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.text.textLayout
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
internal class SnackMessageTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a message is read politely by TalkBack as it shows")
    fun aMessageIsReadPolitelyAsItShows() = extension.use {
        // Given
        var undone = false
        var closed = false
        showMessage(fontScale = 1f, onUndo = { undone = true }, onClose = { closed = true })

        // Then the message is a polite live region: TalkBack reads it when it shows, after what it is reading
        onNode(
            hasAnyDescendant(hasText(TEXT)) and
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        ).assertIsDisplayed()
        // And its action and its cross are buttons of 48 dp, which do what they say
        onNode(hasText(ACTION) and hasClickAction()).assertHeightIsAtLeast(BloxSizes.minTouchTarget).performClick()
        onNode(hasContentDescription(CLOSE) and hasClickAction())
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .performClick()
        assertThat(undone).isTrue()
        assertThat(closed).isTrue()
    }

    @Test
    @DisplayName("in a font twice as large the action of a message goes under its text, no word cut")
    fun inALargeFontTheActionGoesUnderTheText() = extension.use {
        // Given a font twice as large, on 360 dp
        showMessage(fontScale = 2f)

        // Then the action under the text, every word whole
        val text = bounds(hasText(TEXT))
        val action = bounds(hasText(ACTION) and hasClickAction())
        assertThat(action.top).isGreaterThanOrEqualTo(text.bottom)
        assertWordsWhole(textLayout(TEXT))
        assertWordsWhole(textLayout(ACTION))
        assertThat(textLayout(TEXT).hasVisualOverflow).isFalse()
    }

    /** The message of a deletion on a phone of 360 dp, the font scaled by [fontScale], in the Colorful theme. */
    private fun ComposeContext.showMessage(
        fontScale: Float,
        onUndo: () -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    Box(modifier = Modifier.width(360.dp).padding(horizontal = 16.dp)) {
                        SnackMessage(
                            text = TEXT,
                            action = MessageAction(label = ACTION, onClick = onUndo),
                            onClose = onClose,
                        )
                    }
                }
            }
        }
    }

    /** Where the node that [matcher] finds stands. */
    private fun ComposeContext.bounds(matcher: SemanticsMatcher): Rect =
        onNode(matcher).fetchSemanticsNode().boundsInRoot

    private companion object {
        const val TEXT = "« Korobeïniki variée » est supprimée."
        const val ACTION = "Annuler"
        const val CLOSE = "Fermer le message"
    }
}
