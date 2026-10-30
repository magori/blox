package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.text.textLayout
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ScreenBarTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("the bar of a screen reads its title as a heading after its back disc of 48 dp")
    fun theBarOfAScreenReadsItsTitleAsAHeadingAfterItsBackDisc(kind: BloxThemeKind) = extension.use {
        // Given
        var backs = 0
        setContent {
            BloxTheme(kind = kind) { ScreenBar(title = TITLE, backDescription = BACK, onBack = { backs++ }) }
        }

        // Then the back disc, a button of 48 dp, then the title, a heading, 12 dp after it
        onNodeWithContentDescription(BACK)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        onNodeWithText(TITLE).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        assertThat(readOrder(onRoot().fetchSemanticsNode())).containsExactly(BACK, TITLE)
        val back = onNodeWithContentDescription(BACK).getBoundsInRoot()
        val title = onNodeWithText(TITLE).getBoundsInRoot()
        // Within a pixel of a dense screen, where the dp do not fall on whole pixels
        assertThat((title.left - back.right).value).isCloseTo(BloxSpaces.space3.value, within(0.5f))

        // When
        onNodeWithContentDescription(BACK).performClick()

        // Then
        runOnIdle { assertThat(backs).isEqualTo(1) }
    }

    @Test
    @DisplayName("a long title stays on one line, shrunk, beside the back disc")
    fun aLongTitleStaysOnOneLineShrunkBesideTheBackDisc() = extension.use {
        // Given the longest title of a screen bar, on 300 dp, in a font twice as large, in the pixel font of Original,
        // the widest (in Colorful, it holds there at its full size)
        val kind = BloxThemeKind.ORIGINAL
        var fullSize = TextUnit.Unspecified
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = kind) {
                    fullSize = BloxTheme.typography.screenTitle.fontSize
                    ScreenBar(
                        title = LONG_TITLE,
                        backDescription = BACK,
                        onBack = {},
                        modifier = Modifier.width(300.dp),
                    )
                }
            }
        }

        // Then the title holds whole on one line, within its place, smaller than its size
        val layout = textLayout(LONG_TITLE)
        assertThat(layout.layoutInput.style.fontSize.value).isLessThan(fullSize.value)
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.getLineEnd(0)).isEqualTo(LONG_TITLE.length)
        assertThat(layout.getLineRight(0)).isLessThanOrEqualTo(layout.size.width.toFloat())
        // And it does not run under the back disc, nor out of the bar
        val back = onNodeWithContentDescription(BACK).getBoundsInRoot()
        val title = onNodeWithText(LONG_TITLE).getBoundsInRoot()
        assertThat(title.left).isGreaterThanOrEqualTo(back.right)
        assertThat(title.right.value).isLessThanOrEqualTo(300f + 0.5f)
    }

    /** The words TalkBack reads, in its order. */
    private fun readOrder(node: SemanticsNode): List<String> {
        val own = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        return own + node.children.flatMap { readOrder(it) }
    }

    private companion object {
        const val TITLE = "Mes musiques"
        const val BACK = "Retour"
        const val LONG_TITLE = "Mes musiques"
    }
}
