package ch.magori.blox.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.test.RecompositionRecorder
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/*
 * The equalizer moves on the clock of Compose, as an infinite animation: the tests stop such an animation while their
 * clock advances by itself, and draw only then. So the clock is moved by hand while the equalizer moves; let go, it
 * stops the equalizer where it stands, which is then drawn and compared with the same equalizer composed afresh, at
 * its first pose.
 */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class EqualizerTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @JvmField
    @RegisterExtension
    val recompositions = RecompositionRecorder()

    /** Changing it composes the equalizer again from scratch, at its first pose. */
    private var generation by mutableIntStateOf(0)

    @Test
    @DisplayName("the equalizer is left out by TalkBack")
    fun theEqualizerIsLeftOutByTalkBack() = extension.use {
        // Given
        setContent { BloxTheme { Equalizer(Modifier.testTag(EQUALIZER)) } }

        // Then three bars of 3 dp, 2 dp apart, 14 dp high, that TalkBack does not reach
        onNodeWithTag(EQUALIZER, useUnmergedTree = true)
            .assertWidthIsEqualTo(13.dp)
            .assertHeightIsEqualTo(14.dp)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
    }

    @Test
    @DisplayName("nothing recomposes while the equalizer moves")
    fun nothingRecomposesWhileTheEqualizerMoves() = extension.use {
        // Given an equalizer, on a clock moved by hand
        showEqualizer(BloxThemeKind.COLORFUL, reduced = false)
        recompositions.start()

        // When a quarter of a second goes by
        repeat(QUARTER_OF_A_SECOND) { mainClock.advanceTimeByFrame() }

        // Then nothing composed again
        assertThat(recompositions.recomposed).isEmpty()
        // And its bars moved: they no longer stand as at the start
        assertThat(movedFromItsFirstPose()).isTrue()
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource("reduced effects, COLORFUL, true", "Original, ORIGINAL, false")
    @DisplayName("in reduced effects or in Original the equalizer stands still")
    fun inReducedEffectsOrInOriginalTheEqualizerStandsStill(
        case: String,
        kind: BloxThemeKind,
        reduced: Boolean,
    ) = extension.use {
        // Given an equalizer, on a clock moved by hand
        showEqualizer(kind, reduced)

        // When a quarter of a second goes by
        repeat(QUARTER_OF_A_SECOND) { mainClock.advanceTimeByFrame() }

        // Then its bars stand as at the start
        assertThat(movedFromItsFirstPose()).describedAs(case).isFalse()
    }

    /** An equalizer in the theme [kind], [reduced] effects or not, its first frame passed, on a clock moved by hand. */
    private fun ComposeContext.showEqualizer(
        kind: BloxThemeKind,
        reduced: Boolean,
    ) {
        mainClock.autoAdvance = false
        setContent {
            BloxTheme(kind = kind, effects = BloxEffects(reduced = reduced, forcedBySystem = false)) {
                key(generation) { Equalizer(Modifier.testTag(EQUALIZER)) }
            }
        }
        mainClock.advanceTimeByFrame()
    }

    /** Whether the equalizer, stopped where it stands, is drawn otherwise than when composed afresh. */
    private fun ComposeContext.movedFromItsFirstPose(): Boolean {
        mainClock.autoAdvance = true
        val stopped = drawn()
        runOnIdle { generation++ }
        return !stopped.sameAs(drawn())
    }

    private fun ComposeContext.drawn(): Bitmap =
        onNodeWithTag(EQUALIZER, useUnmergedTree = true).captureToImage().asAndroidBitmap()

    private companion object {
        const val EQUALIZER = "equalizer"

        /** Frames of 1/60 s. */
        const val QUARTER_OF_A_SECOND = 15
    }
}
