package ch.magori.blox.feature.game.controls

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HeldControlsTest {
    private val changes = mutableListOf<ControlChange>()
    private var pauses = 0
    private val controls = HeldControls { changes += it }

    private fun key(
        key: Key,
        type: KeyEventType,
        repeatCount: Int = 0,
        source: KeySource = KeySource.KEYBOARD,
    ): Boolean = controls.key(key, type, repeatCount, source) { pauses++ }

    @Test
    fun `the up of the cross of a pad is kept from the system and holds nothing`() {
        // When
        val down = key(Key.DirectionUp, KeyEventType.KeyDown, source = KeySource.PAD)
        val up = key(Key.DirectionUp, KeyEventType.KeyUp, source = KeySource.PAD)

        // Then
        assertThat(listOf(down, up)).containsOnly(true)
        assertThat(changes).isEmpty()
    }

    @Test
    fun `a key of the game is kept from the system, another key is left to it`() {
        // When
        val escape = key(Key.Escape, KeyEventType.KeyDown)
        val left = key(Key.DirectionLeft, KeyEventType.KeyDown)
        val leftRepeat = key(Key.DirectionLeft, KeyEventType.KeyDown, repeatCount = 1)
        val leftUp = key(Key.DirectionLeft, KeyEventType.KeyUp)
        val tab = key(Key.Tab, KeyEventType.KeyDown)
        val back = key(Key.Back, KeyEventType.KeyDown)

        // Then
        assertThat(listOf(escape, left, leftRepeat, leftUp)).containsOnly(true)
        assertThat(listOf(tab, back)).containsOnly(false)
    }

    @Test
    fun `the pause is asked once, as Escape goes down, never as it comes back up`() {
        // When Escape goes down, repeats, then comes back up
        key(Key.Escape, KeyEventType.KeyDown)
        val afterDown = pauses
        key(Key.Escape, KeyEventType.KeyDown, repeatCount = 1)
        key(Key.Escape, KeyEventType.KeyUp)

        // Then
        assertThat(afterDown).isEqualTo(1)
        assertThat(pauses).isEqualTo(1)
        assertThat(changes).isEmpty()
    }

    @Test
    fun `a key going down then up presses then releases its button`() {
        // When
        key(Key.DirectionLeft, KeyEventType.KeyDown)
        val held = controls.heldButtons
        key(Key.DirectionLeft, KeyEventType.KeyUp)

        // Then
        assertThat(held).containsExactly(ControlButton.LEFT)
        assertThat(
            changes,
        ).containsExactly(ControlChange.Pressed(ControlButton.LEFT), ControlChange.Released(ControlButton.LEFT))
        assertThat(controls.heldButtons).isEmpty()
    }
}
