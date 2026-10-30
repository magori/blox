package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.test.ExperimentalTestApi
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ColorfulThemeTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("the colorful theme draws in the colorful variant of the system mode, keeping the effects around it")
    fun theColorfulThemeDrawsInTheColorfulVariantKeepingTheEffectsAroundIt() = extension.use {
        // Given the Original theme chosen, at night, with reduced effects
        val reduced = BloxEffects(reduced = true, forcedBySystem = false)
        var variant: BloxThemeVariant? = null
        var effects: BloxEffects? = null

        // When a screen imposes the Colorful theme
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL, darkSystem = true, effects = reduced) {
                ColorfulTheme(darkSystem = true) {
                    variant = BloxTheme.variant
                    effects = BloxTheme.effects
                }
            }
        }

        // Then it is drawn in the Colorful night, with the effects of the player
        assertThat(variant).isEqualTo(BloxThemeVariant.COLORFUL_DARK)
        assertThat(effects).isEqualTo(reduced)
    }
}
