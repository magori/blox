package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
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
internal class LabelledControlTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("in a normal font, the gauge stands beside its label, at the end of the line")
    fun inANormalFontTheGaugeStandsBesideItsLabel() = extension.use {
        // Given the gauge of the level in the card of the home, on a phone of 360 dp, in Colorful (the pixel font of
        // Original is too wide for both on one line)
        showLevelRow(BloxThemeKind.COLORFUL, fontScale = 1f)

        // Then
        val row = onNodeWithTag(ROW).getUnclippedBoundsInRoot()
        val label = onNodeWithText("Niveau").getUnclippedBoundsInRoot()
        val gauge = onNodeWithContentDescription("Niveau de départ").getUnclippedBoundsInRoot()
        assertThat(gauge.left).isGreaterThanOrEqualTo(label.right)
        assertThat(gauge.top).isLessThan(label.bottom)
        assertThat(gauge.right.value).isCloseTo(row.right.value, within(PIXEL_ROUNDING))
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large, the label goes above and the gauge under it, centred on the line")
    fun inALargeFontTheGaugeGoesUnderItsLabelCentred(kind: BloxThemeKind) = extension.use {
        // Given the gauge of the level in the card of the home, on a phone of 360 dp, in a font twice as large
        showLevelRow(kind, fontScale = 2f)

        // Then
        val row = onNodeWithTag(ROW).getUnclippedBoundsInRoot()
        val label = onNodeWithText("Niveau").getUnclippedBoundsInRoot()
        val gauge = onNodeWithContentDescription("Niveau de départ").getUnclippedBoundsInRoot()
        assertThat(gauge.top).isGreaterThanOrEqualTo(label.bottom)
        // Its bars, between − and +, in the middle of the line
        val bars = onNodeWithTag(LEVEL_GAUGE_BARS_TAG, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val before = bars.left - row.left
        val after = row.right - bars.right
        assertThat(before.value).isCloseTo(after.value, within(1f))
    }

    /** The row of the level of the home, in the [kind] of theme, its font scaled by [fontScale]. */
    private fun ComposeContext.showLevelRow(
        kind: BloxThemeKind,
        fontScale: Float,
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    LabelledControl(label = "Niveau", modifier = Modifier.width(CARD_WIDTH).testTag(ROW)) {
                        LevelGauge(level = 5, levels = 0..9, onLevelChange = {}, description = "Niveau de départ")
                    }
                }
            }
        }
    }

    private companion object {
        const val ROW = "row"

        /** The inside of the card of the settings on a phone of 360 dp: 16 dp of page and 16 dp of card each side. */
        val CARD_WIDTH = 296.dp

        /** The rounding of an edge to the pixel, in dp. */
        const val PIXEL_ROUNDING = 0.5f
    }
}
