package ch.magori.blox.feature.cartridges

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.text.textLayout
import ch.magori.blox.core.designsystem.theme.BloxSizes
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
internal class CartridgesScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest(name = "{0}")
    @EnumSource(Cartridge::class)
    @DisplayName("TalkBack reads the name of each cartridge, as a button of 48 dp at least")
    fun talkBackReadsTheNameOfEachCartridge(cartridge: Cartridge) = extension.use {
        // Given
        showCartridges()

        // Then
        onNode(hasText(nameOf(cartridge)) and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
    }

    @Test
    @DisplayName("the Classic cartridge opens the Classic")
    fun theClassicCartridgeOpensTheClassic() = extension.use {
        // Given
        val opened = mutableListOf<Cartridge>()
        showCartridges(onOpen = opened::add)

        // When
        onNode(hasText(nameOf(Cartridge.CLASSIC)) and hasClickAction()).performClick()

        // Then
        runOnIdle { assertThat(opened).containsExactly(Cartridge.CLASSIC) }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("TalkBack reads « Choix du jeu » as the heading of the screen")
    fun talkBackReadsTheTitleAsTheHeading(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind)

        // Then: as text in Original, as the name of the logo in Colorful
        val title = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.feature_cartridges_title)
        onNode(isHeading() and (hasText(title) or hasContentDescription(title))).assertExists()
    }

    @Test
    @DisplayName("a cartridge held down goes 2 dp down, as a bubble button")
    fun aCartridgeHeldDownGoesDown() = extension.use {
        // Given a cartridge at rest, on a clock that only moves when told
        mainClock.autoAdvance = false
        showCartridges()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        waitForIdle()
        val cartridge = onNode(hasText(nameOf(Cartridge.CLASSIC)) and hasClickAction())
        val atRest = cartridge.getBoundsInRoot().top

        // When it is held down, past the wait of a press in a column that scrolls
        cartridge.performTouchInput { down(center) }
        mainClock.advanceTimeBy(HOLD_MILLIS)

        // Then it is 2 dp lower
        assertThat(
            (cartridge.getBoundsInRoot().top - atRest).value,
        ).isCloseTo(BloxSizes.pressDrop.value, within(HALF_DP))
        cartridge.performTouchInput { up() }
    }

    @Test
    @DisplayName(
        "TalkBack reads nothing of the console: the only buttons, focusable or disabled nodes are the cartridges",
    )
    fun talkBackReadsNothingOfTheConsole() = extension.use {
        // Given
        showCartridges()

        // Then, in the tree TalkBack reads: the eject, disabled, is neither found nor focused
        val names = Cartridge.entries.map(::nameOf)
        val buttons = onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .fetchSemanticsNodes()
        assertThat(buttons.map { it.config[SemanticsProperties.Text].joinToString() })
            .containsExactlyInAnyOrderElementsOf(names)
        assertThat(onAllNodes(isFocusable()).fetchSemanticsNodes()).hasSize(names.size)
        assertThat(onAllNodes(isNotEnabled()).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    @DisplayName("in Colorful, on 360 dp, the two cartridges stand side by side, their tops level")
    fun inColorfulTheCartridgesStandSideBySide() = extension.use {
        // Given
        showCartridges(width = NARROW_PHONE)

        // Then
        val (first, second) = Cartridge.entries.map { onNodeWithTag(it.tag).getBoundsInRoot() }
        assertThat(second.top).isEqualTo(first.top)
        assertThat(second.left).isGreaterThanOrEqualTo(first.right)
    }

    @Test
    @DisplayName("in Original, in a font twice as large, on 360 dp, the cartridges stand one above the other")
    fun inOriginalInALargeFontTheCartridgesAreStacked() = extension.use {
        // Given
        showCartridges(kind = BloxThemeKind.ORIGINAL, width = NARROW_PHONE, fontScale = 2f)

        // Then
        val (first, second) = Cartridge.entries.map { onNodeWithTag(it.tag).getBoundsInRoot() }
        assertThat(second.top).isGreaterThanOrEqualTo(first.bottom)
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("each cartridge bears the mascot of its game in Colorful, none in Original, which is frozen")
    fun eachCartridgeBearsTheMascotOfItsGameInColorful(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind)

        // Then: a decoration, inside the button that TalkBack reads by the name of the game
        Cartridge.entries.forEach { cartridge ->
            val mascots = onAllNodesWithTag(cartridge.mascotTag, useUnmergedTree = true).fetchSemanticsNodes()
            assertThat(mascots).hasSize(if (kind == BloxThemeKind.COLORFUL) 1 else 0)
        }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large, on 360 dp, the name of each cartridge stays whole, on one line, unclipped")
    fun inALargeFontTheNameOfEachCartridgeStaysWhole(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind, width = NARROW_PHONE, fontScale = 2f)

        // Then
        Cartridge.entries.forEach { cartridge ->
            val layout = textLayout(nameOf(cartridge))
            assertWordsWhole(layout)
            assertThat(layout.lineCount).isEqualTo(1)
            assertThat(layout.hasVisualOverflow).describedAs(nameOf(cartridge)).isFalse()
        }
    }

    private fun ComposeContext.showCartridges(
        onOpen: (Cartridge) -> Unit = {},
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    CartridgesScreen(cartridges = Cartridge.entries, onOpen = onOpen, modifier = Modifier.width(width))
                }
            }
        }
    }

    private fun nameOf(cartridge: Cartridge): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(cartridge.label)
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp

/** The slack of the drop of a pressed button, rounded to whole pixels. */
private const val HALF_DP = 0.5f

/** Longer than the wait before a press shows in a column that scrolls (`tapTimeout`, 100 ms). */
private const val HOLD_MILLIS = 300L
