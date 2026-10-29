package ch.magori.blox.feature.locksmith

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.locksmith.PadlockChords
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ChordPillsTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @ValueSource(floats = [1f, 2f])
    @DisplayName("on a phone of 360 dp, each pill of 3 to 8 chords is whole within the width, and « cadence » whole")
    fun on360DpEachPillIsWholeWithinTheWidth(fontScale: Float) = extension.use {
        // Given the band of a padlock of 3 pieces, none heard, in the width a phone of 360 dp leaves it, in a font of
        // that scale
        val width = 360.dp - SIDE_MARGIN * 2
        val shown = mutableStateOf(PIECES.first to 0)
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    val (count, heard) = shown.value
                    ChordPills(
                        numerals = PadlockChords.progression(count).numerals,
                        heard = heard,
                        modifier = Modifier.width(width).testTag(BAND),
                    )
                }
            }
        }

        PIECES.forEach { count ->
            listOf(0, count / 2).forEach { heard ->
                // When the padlock holds that many pieces, that many chords heard
                shown.value = count to heard
                waitForIdle()

                // Then each pill lies between the edges of the band, in pixels as laid out, its label whole within it
                val band = bounds(hasTestTag(BAND))
                repeat(count) { rank ->
                    val pill = hasTestTag(chordPillTestTag(rank))
                    val what = "pill $rank of $count, $heard heard"
                    val bounds = bounds(pill)
                    assertThat(bounds.left).describedAs("left of $what").isGreaterThanOrEqualTo(band.left)
                    assertThat(bounds.right).describedAs("right of $what").isLessThanOrEqualTo(band.right)
                    assertWhole(layoutOf(hasParent(pill)), "label of $what")
                }
                // And the word « cadence » whole too, never cut in the middle
                val cadence = InstrumentationRegistry.getInstrumentation().targetContext
                    .getString(R.string.feature_locksmith_cadence)
                assertWhole(layoutOf(hasText(cadence)), "« $cadence » with $count chords, $heard heard")
            }
        }
    }

    /** The bounds in pixels of the node that [matcher] finds, as laid out, not as clipped. */
    private fun ComposeContext.bounds(matcher: SemanticsMatcher): Rect {
        val node = onNode(matcher).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }

    /** That the text of [layout], told as [what], is drawn whole: on one line, nothing beyond its bounds. */
    private fun assertWhole(
        layout: TextLayoutResult,
        what: String,
    ) {
        assertThat(layout.lineCount).describedAs("lines of $what").isEqualTo(1)
        assertThat(
            layout.getLineRight(0),
        ).describedAs("right of $what").isLessThanOrEqualTo(layout.size.width.toFloat())
    }

    /** The layout of the text that [matcher] finds, as drawn. */
    private fun ComposeContext.layoutOf(matcher: SemanticsMatcher): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }

    private companion object {
        /** The pieces of a padlock, and so its chords: 3 to 8. */
        val PIECES = 3..8

        /** The tag of the band, set by the test on the root of the pills. */
        const val BAND = "chord_band"
    }
}
