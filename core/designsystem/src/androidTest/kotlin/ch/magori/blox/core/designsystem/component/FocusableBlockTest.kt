package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.math.roundToInt

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class FocusableBlockTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("from the keyboard, a focusable block takes the focus, read in one, and bears the ring of the buttons")
    fun aFocusableBlockFocusedBearsTheRing() = extension.use {
        // Given two lines of text in a focusable block, the keyboard in use
        var ring = Color.Unspecified
        var page = Color.Unspecified
        val focus = FocusRequester()
        setContent {
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                ring = BloxTheme.buttons.focus
                page = BloxTheme.palette.page
                Box(Modifier.background(page).padding(24.dp)) {
                    FocusableBlock(Modifier.testTag(BLOCK).focusRequester(focus)) {
                        Text("Aucun jardin gardé.")
                        Text("Garde le jardin en cours.")
                    }
                }
            }
        }
        onNodeWithTag(BLOCK).assertIsNotFocused()
        assertThat(pixelLeftOfBlock()).isEqualTo(page)

        // When
        runOnIdle { focus.requestFocus() }

        // Then it is focused, its two lines in one node, ringed off its edge
        onNodeWithTag(BLOCK).assertIsFocused()
        onNode(hasText("Aucun jardin gardé.") and hasText("Garde le jardin en cours.")).assertIsFocused()
        assertThat(pixelLeftOfBlock()).isEqualTo(ring)
    }

    /** The colour drawn at the middle of the ring's width, left of the block, half-way down. */
    private fun ComposeContext.pixelLeftOfBlock(): Color {
        val bounds = onNodeWithTag(BLOCK).fetchSemanticsNode().boundsInRoot
        val offset = with(density) { (BloxBorders.focusGap + BloxBorders.focus / 2).toPx() }
        val image = onRoot().captureToImage().toPixelMap()
        return image[(bounds.left - offset).roundToInt(), bounds.center.y.roundToInt()]
    }

    private companion object {
        const val BLOCK = "block"
    }
}
