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
    fun `the choice of the player comes back once Android animates again`() {
        // Given full effects chosen, forced reduced while Android removed the animations
        val forced = BloxEffects.resolve(reducedChosen = false, systemAnimates = false)

        // When Android animates again: the choice was never overwritten, only resolved anew
        val back = BloxEffects.resolve(reducedChosen = false, systemAnimates = true)

        // Then
        assertThat(forced.reduced).isTrue()
        assertThat(back).isEqualTo(BloxEffects.Full)
    }
}
