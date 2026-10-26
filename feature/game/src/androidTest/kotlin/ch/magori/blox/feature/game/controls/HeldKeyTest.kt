package ch.magori.blox.feature.game.controls

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class HeldKeyTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a held key goes 2 dp down with its tag and what TalkBack reads, reduced effects or not")
    fun aHeldKeyGoesDownWithItsTagReducedEffectsOrNot() = extension.use {
        // Given the keys B and A, in full effects, then in reduced effects
        val controls = HeldControls(onChange = {})
        var effects by mutableStateOf(BloxEffects.Full)
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL, effects = effects) { RotationPad(controls) }
        }

        listOf(BloxEffects.Full, BloxEffects(reduced = true, forcedBySystem = true)).forEach { shown ->
            effects = shown
            waitForIdle()
            val atRest = keyTop()

            // When A is held
            runOnIdle { controls.press(FINGER, ControlButton.A) }
            val held = keyTop()
            runOnIdle { controls.lift(FINGER) }

            // Then it is 2 dp lower: the drop is no animation, and the only cue of A held in the day (KeyColorsTest)
            assertThat(held - atRest).describedAs("in %s", shown).isCloseTo(2f, within(0.5f))
        }
    }

    /** The top of the key A, in dp, where it is drawn. */
    private fun ComposeContext.keyTop(): Float =
        onNodeWithTag(controlTestTag(ControlButton.A)).getBoundsInRoot().top.value

    private companion object {
        const val FINGER = 1L
    }
}
