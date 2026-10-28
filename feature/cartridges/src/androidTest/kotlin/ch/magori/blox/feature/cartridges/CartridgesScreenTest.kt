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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

    @ParameterizedTest(name = "{0}")
    @EnumSource(Cartridge::class)
    @DisplayName("a cartridge tapped is handed over, to open its game")
    fun aCartridgeTappedIsHandedOver(cartridge: Cartridge) = extension.use {
        // Given
        val opened = mutableListOf<Cartridge>()
        showCartridges(onOpen = opened::add)

        // When
        onNode(hasText(nameOf(cartridge)) and hasClickAction()).performClick()

        // Then
        runOnIdle { assertThat(opened).containsExactly(cartridge) }
    }

    @ParameterizedTest(name = "under {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("the Graine du jour, new, bears the sticker NOUVELLE, which TalkBack reads after its name")
    fun theGraineDuJourNewBearsTheSticker(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind, newCartridges = setOf(Cartridge.DAILY))

        // Then: the sticker is a decoration, its word read as the state of the button
        val sticker = string(R.string.feature_cartridges_new)
        onNodeWithText(sticker.uppercase(), useUnmergedTree = true).assertIsDisplayed()
        onNode(hasText(nameOf(Cartridge.DAILY)) and hasClickAction()).assert(hasStateDescription(sticker))
    }

    @Test
    @DisplayName("a cartridge that is not new bears no sticker, and TalkBack reads its name alone")
    fun aCartridgeNotNewBearsNoSticker() = extension.use {
        // Given the Graine du jour already tried today
        showCartridges(newCartridges = emptySet())

        // Then
        val sticker = string(R.string.feature_cartridges_new)
        assertThat(onAllNodesWithText(sticker.uppercase(), useUnmergedTree = true).fetchSemanticsNodes()).isEmpty()
        Cartridge.entries.forEach { cartridge ->
            onNode(hasText(nameOf(cartridge)) and hasClickAction())
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        }
    }

    @ParameterizedTest(name = "under {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("TalkBack reads « Choix du jeu » as the heading of the screen")
    fun talkBackReadsTheTitleAsTheHeading(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind)

        // Then
        val title = string(R.string.feature_cartridges_title)
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
    @DisplayName("the only buttons, focusable or disabled nodes of the screen are the cartridges")
    fun theOnlyButtonsAreTheCartridges() = extension.use {
        // Given, the Graine du jour new: its sticker is no button
        showCartridges(newCartridges = setOf(Cartridge.DAILY))

        // Then, in the tree TalkBack reads
        val names = Cartridge.entries.map(::nameOf)
        val buttons = onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .fetchSemanticsNodes()
        assertThat(buttons.map { it.config[SemanticsProperties.Text].joinToString() })
            .containsExactlyInAnyOrderElementsOf(names)
        assertThat(onAllNodes(isFocusable()).fetchSemanticsNodes()).hasSize(names.size)
        assertThat(onAllNodes(isNotEnabled()).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    @DisplayName("in Colorful, on 360 dp, the four cartridges stand on two full rows of two, as wide as each other")
    fun inColorfulTheFourCartridgesStandOnTwoFullRows() = extension.use {
        // Given
        showCartridges(width = NARROW_PHONE)

        // Then: the Classic and the Locksmith side by side, the Jardin musical and the Graine du jour under them
        val (first, second, third, fourth) = Cartridge.entries.map { onNodeWithTag(it.tag).getUnclippedBoundsInRoot() }
        assertThat(second.top).isEqualTo(first.top)
        assertThat(second.left).isGreaterThanOrEqualTo(first.right)
        assertThat(third.top).isGreaterThanOrEqualTo(first.bottom)
        assertThat(fourth.top).isEqualTo(third.top)
        assertThat(third.left).isEqualTo(first.left)
        assertThat(fourth.left).isEqualTo(second.left)
        listOf(second, third, fourth).forEach {
            assertThat((it.right - it.left).value).isCloseTo((first.right - first.left).value, within(HALF_DP))
        }
    }

    @Test
    @DisplayName("on 360 dp, of an odd number of cartridges, the last one stands alone below, centred")
    fun ofAnOddNumberOfCartridgesTheLastOneStandsAloneCentred() = extension.use {
        // Given three cartridges, as before the Jardin musical
        val three = listOf(Cartridge.CLASSIC, Cartridge.LOCKSMITH, Cartridge.DAILY)
        showCartridges(width = NARROW_PHONE, cartridges = three)

        // Then: the Classic and the Locksmith side by side, their tops level, the Graine du jour under them, centred
        val (first, second, third) = three.map { onNodeWithTag(it.tag).getBoundsInRoot() }
        assertThat(second.top).isEqualTo(first.top)
        assertThat(second.left).isGreaterThanOrEqualTo(first.right)
        assertThat(third.top).isGreaterThanOrEqualTo(first.bottom)
        assertThat((third.right - third.left).value).isCloseTo((first.right - first.left).value, within(HALF_DP))
        assertThat(((third.left + third.right) / 2).value)
            .isCloseTo(((first.left + second.right) / 2).value, within(HALF_DP))
    }

    @Test
    @DisplayName(
        "in a font twice as large, on 240 dp, the cartridges stand in one column: two names do not hold side by side",
    )
    fun inALargeFontOnANarrowWindowTheCartridgesAreStacked() = extension.use {
        // Given
        showCartridges(width = TOO_NARROW, fontScale = 2f)

        // Then, the last ones below the edge of the screen, which scrolls
        Cartridge.entries.map { onNodeWithTag(it.tag).getUnclippedBoundsInRoot() }.zipWithNext { above, below ->
            assertThat(below.top).isGreaterThanOrEqualTo(above.bottom)
        }
    }

    @ParameterizedTest(name = "under {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "each cartridge bears the mascot of its game, whatever the theme: the choice of the game is drawn in Colorful",
    )
    fun eachCartridgeBearsTheMascotOfItsGame(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind)

        // Then: a decoration, inside the button that TalkBack reads by the name of the game
        Cartridge.entries.forEach { cartridge ->
            val mascots = onAllNodesWithTag(cartridge.mascotTag, useUnmergedTree = true).fetchSemanticsNodes()
            assertThat(mascots).hasSize(1)
        }
    }

    @ParameterizedTest(name = "under {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, on 360 dp, the name of each cartridge stays whole, on two lines at most, unclipped",
    )
    fun inALargeFontTheNameOfEachCartridgeStaysWhole(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind, width = NARROW_PHONE, fontScale = 2f)

        // Then
        val (first, second) = Cartridge.entries.map { onNodeWithTag(it.tag).getBoundsInRoot() }
        assertThat(second.top).describedAs("still two columns").isEqualTo(first.top)
        Cartridge.entries.forEach { cartridge ->
            val layout = textLayout(nameOf(cartridge))
            assertWordsWhole(layout)
            assertThat(layout.lineCount).isLessThanOrEqualTo(2)
            assertThat(layout.hasVisualOverflow).describedAs(nameOf(cartridge)).isFalse()
        }
    }

    private fun ComposeContext.showCartridges(
        onOpen: (Cartridge) -> Unit = {},
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
        newCartridges: Set<Cartridge> = emptySet(),
        cartridges: List<Cartridge> = Cartridge.entries,
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    CartridgesScreen(
                        cartridges = cartridges,
                        newCartridges = newCartridges,
                        onOpen = onOpen,
                        modifier = Modifier.width(width),
                    )
                }
            }
        }
    }

    private fun nameOf(cartridge: Cartridge): String = string(cartridge.label)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp

/** A window too narrow for the names of two cartridges side by side in a font twice as large. */
private val TOO_NARROW = 240.dp

/** The slack of the drop of a pressed button, rounded to whole pixels. */
private const val HALF_DP = 0.5f

/** Longer than the wait before a press shows in a column that scrolls (`tapTimeout`, 100 ms). */
private const val HOLD_MILLIS = 300L
