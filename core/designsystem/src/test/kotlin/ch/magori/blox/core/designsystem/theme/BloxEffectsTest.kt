package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class BloxEffectsTest {
    @Test
    fun `effects are reduced when the player asks for it`() {
        // When the player turns Effets réduits on, Android animating
        val effects = BloxEffects.resolve(reducedChosen = true, systemAnimates = true)

        // Then
        assertThat(effects).isEqualTo(BloxEffects(reduced = true, forcedBySystem = false))
    }

    @ParameterizedTest(name = "chosen {0}")
    @ValueSource(booleans = [false, true])
    fun `effects are reduced and forced when Android removes the animations, whatever the choice`(
        reducedChosen: Boolean,
    ) {
        // When Android removes the animations (Supprimer les animations, or an animator duration scale of 0)
        val effects = BloxEffects.resolve(reducedChosen, systemAnimates = false)

        // Then
        assertThat(effects).isEqualTo(BloxEffects(reduced = true, forcedBySystem = true))
    }

    @Test
    fun `the same choice of full effects resolves to full effects again once Android animates`() {
        // Given full effects chosen, resolved to forced reduced effects while Android removes the animations
        val forced = BloxEffects.resolve(reducedChosen = false, systemAnimates = false)

        // When the same choice is resolved once Android animates again (that the choice is never overwritten while
        // forced is SettingsScreenTest's: a forced switch asks for nothing)
        val back = BloxEffects.resolve(reducedChosen = false, systemAnimates = true)

        // Then
        assertThat(forced.reduced).isTrue()
        assertThat(back).isEqualTo(BloxEffects.Full)
    }
}
