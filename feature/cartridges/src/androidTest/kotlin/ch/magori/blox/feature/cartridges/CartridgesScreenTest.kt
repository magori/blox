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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
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
    @DisplayName("in a font twice as large, on 360 dp, the name of each cartridge stays whole, on one line")
    fun inALargeFontTheNameOfEachCartridgeStaysWhole(kind: BloxThemeKind) = extension.use {
        // Given
        showCartridges(kind = kind, width = NARROW_PHONE, fontScale = 2f)

        // Then
        Cartridge.entries.forEach { cartridge ->
            val layout = textLayout(nameOf(cartridge))
            assertWordsWhole(layout)
            assertThat(layout.lineCount).isEqualTo(1)
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
