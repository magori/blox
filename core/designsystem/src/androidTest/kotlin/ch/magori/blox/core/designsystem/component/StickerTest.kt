package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
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
internal class StickerTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a sticker shows its text in capitals")
    fun aStickerShowsItsTextInCapitals(kind: BloxThemeKind) = extension.use {
        // Given
        setContent { BloxTheme(kind = kind) { Sticker(text = "Nouvelle") } }

        // Then
        onNodeWithText("NOUVELLE").assertExists()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large and a narrow place, a sticker of one line shrinks to fit its width")
    fun aStickerOfOneLineShrinksToFitItsWidth(kind: BloxThemeKind) = extension.use {
        // Given
        setContent { Narrow(kind, width = 300.dp) { Sticker(text = "Nouveau meilleur") } }

        // Then on one line, the whole of it within its width, not cut on the right
        val layout = onNodeWithText("NOUVEAU MEILLEUR").textLayout()
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.multiParagraph.intrinsics.maxIntrinsicWidth).isLessThanOrEqualTo(layout.size.width.toFloat())
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large and a narrow place, a sticker of two lines breaks between its words")
    fun aStickerOfTwoLinesBreaksBetweenItsWords(kind: BloxThemeKind) = extension.use {
        // Given
        setContent { Narrow(kind, width = 160.dp) { Sticker(text = "Nouveau meilleur", maxLines = 2) } }

        // Then NOUVEAU on the first line, MEILLEUR on the second
        val layout = onNodeWithText("NOUVEAU MEILLEUR").textLayout()
        assertThat(layout.lineCount).isEqualTo(2)
        assertThat("NOUVEAU MEILLEUR".substring(0, layout.getLineEnd(0)).trim()).isEqualTo("NOUVEAU")
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a sticker in a style without a size, among texts without one, still shows its text")
    fun aStickerWithoutAnySizeStillShowsItsText(kind: BloxThemeKind) = extension.use {
        // Given no size in its style nor in the text around it
        setContent {
            BloxTheme(kind = kind) {
                CompositionLocalProvider(LocalTextStyle provides TextStyle.Default) {
                    Sticker(text = "Parfaite", style = TextStyle.Default)
                }
            }
        }

        // Then
        onNodeWithText("PARFAITE").assertExists()
    }
}

/**
 * [content] in [kind], in a font twice as large, in [width]: 300 dp, too narrow for NOUVEAU MEILLEUR at full size, wide
 * enough at the smallest; 160 dp, too narrow for it on one line even at the smallest, wide enough for NOUVEAU.
 */
@Composable
private fun Narrow(
    kind: BloxThemeKind,
    width: Dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        BloxTheme(kind = kind) { Box(Modifier.width(width)) { content() } }
    }
}

private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val layouts = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
    return layouts.single()
}
