package ch.magori.blox.feature.game.controls

import android.view.InputDevice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class KeySourceTest {
    @Test
    fun `a keyboard, with or without arrows, is a keyboard`() {
        assertThat(keySourceOf(InputDevice.SOURCE_KEYBOARD)).isEqualTo(KeySource.KEYBOARD)
        assertThat(keySourceOf(InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD)).isEqualTo(KeySource.KEYBOARD)
    }

    @Test
    fun `a gamepad or a joystick is a pad, even when it also says it is a keyboard`() {
        assertThat(keySourceOf(InputDevice.SOURCE_GAMEPAD)).isEqualTo(KeySource.PAD)
        assertThat(keySourceOf(InputDevice.SOURCE_JOYSTICK)).isEqualTo(KeySource.PAD)
        assertThat(keySourceOf(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD)).isEqualTo(KeySource.PAD)
        assertThat(keySourceOf(InputDevice.SOURCE_DPAD or InputDevice.SOURCE_JOYSTICK)).isEqualTo(KeySource.PAD)
    }
}
