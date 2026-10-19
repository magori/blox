package ch.magori.blox.feature.game.controls

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

internal class KeyActionTest {
    @ParameterizedTest(name = "{0} holds {2}")
    @MethodSource("moves")
    fun `the arrows and the cross of a pad, ZQSD and WASD hold left, down and right`(
        name: String,
        keyCode: Long,
        button: ControlButton,
    ) {
        assertHolds(Key(keyCode), button)
    }

    @ParameterizedTest(name = "{0} holds A")
    @MethodSource("clockwise")
    fun `the up arrow, X, K and the A button of a pad hold A, the clockwise rotation`(
        name: String,
        keyCode: Long,
    ) {
        assertHolds(Key(keyCode), ControlButton.A)
    }

    @ParameterizedTest(name = "{0} holds B")
    @MethodSource("counterclockwise")
    fun `Z, W and Y, J, Ctrl and the B and X buttons of a pad hold B, the counterclockwise rotation`(
        name: String,
        keyCode: Long,
    ) {
        assertHolds(Key(keyCode), ControlButton.B)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pauseKeys")
    fun `Escape, P and the Start button of a pad pause the game as they go down`(
        name: String,
        keyCode: Long,
    ) {
        // When
        val action = keyAction(Key(keyCode), KeyEventType.KeyDown, repeatCount = 0, source = KeySource.KEYBOARD)

        // Then
        assertThat(action).isEqualTo(KeyAction.Pause)
    }

    @Test
    fun `a pause key coming back up or repeated is kept from the system and does nothing`() {
        // When
        val up = keyAction(Key.Escape, KeyEventType.KeyUp, repeatCount = 0, source = KeySource.KEYBOARD)
        val repeat = keyAction(Key.Escape, KeyEventType.KeyDown, repeatCount = 1, source = KeySource.KEYBOARD)

        // Then
        assertThat(up).isEqualTo(KeyAction.Ignored)
        assertThat(repeat).isEqualTo(KeyAction.Ignored)
    }

    @Test
    fun `a key event neither down nor up does nothing`() {
        // When
        val move = keyAction(Key.DirectionLeft, KeyEventType.Unknown, repeatCount = 0, source = KeySource.KEYBOARD)
        val pause = keyAction(Key.Escape, KeyEventType.Unknown, repeatCount = 0, source = KeySource.KEYBOARD)

        // Then
        assertThat(move).isEqualTo(KeyAction.Ignored)
        assertThat(pause).isEqualTo(KeyAction.Ignored)
    }

    @Test
    fun `the repeats of a held key are kept from the system and do nothing, the engine repeats a held button (DAS)`() {
        // When the keyboard repeats ◀ held down
        val firstRepeat =
            keyAction(Key.DirectionLeft, KeyEventType.KeyDown, repeatCount = 1, source = KeySource.KEYBOARD)
        val laterRepeat =
            keyAction(Key.DirectionLeft, KeyEventType.KeyDown, repeatCount = 30, source = KeySource.KEYBOARD)

        // Then
        assertThat(firstRepeat).isEqualTo(KeyAction.Ignored)
        assertThat(laterRepeat).isEqualTo(KeyAction.Ignored)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("otherKeys")
    fun `another key does nothing in the game, and is left to the system`(
        name: String,
        keyCode: Long,
    ) {
        // When
        val down = keyAction(Key(keyCode), KeyEventType.KeyDown, repeatCount = 0, source = KeySource.KEYBOARD)
        val up = keyAction(Key(keyCode), KeyEventType.KeyUp, repeatCount = 0, source = KeySource.KEYBOARD)

        // Then
        assertThat(down).isNull()
        assertThat(up).isNull()
    }

    @Test
    fun `the up of the cross of a pad does nothing, as on the NES, and is kept from the system`() {
        // When
        val down = keyAction(Key.DirectionUp, KeyEventType.KeyDown, repeatCount = 0, source = KeySource.PAD)
        val up = keyAction(Key.DirectionUp, KeyEventType.KeyUp, repeatCount = 0, source = KeySource.PAD)

        // Then
        assertThat(down).isEqualTo(KeyAction.Ignored)
        assertThat(up).isEqualTo(KeyAction.Ignored)
    }

    @Test
    fun `the other directions and buttons of a pad play as on a keyboard`() {
        assertHolds(Key.DirectionLeft, ControlButton.LEFT, KeySource.PAD)
        assertHolds(Key.DirectionDown, ControlButton.DOWN, KeySource.PAD)
        assertHolds(Key.ButtonA, ControlButton.A, KeySource.PAD)
        assertHolds(Key.ButtonB, ControlButton.B, KeySource.PAD)
    }

    /** [key] presses [button] going down, and releases it coming back up. */
    private fun assertHolds(
        key: Key,
        button: ControlButton,
        source: KeySource = KeySource.KEYBOARD,
    ) {
        // When
        val down = keyAction(key, KeyEventType.KeyDown, repeatCount = 0, source = source)
        val up = keyAction(key, KeyEventType.KeyUp, repeatCount = 0, source = source)

        // Then
        assertThat(down).isEqualTo(KeyAction.Press(button))
        assertThat(up).isEqualTo(KeyAction.Release)
    }

    companion object {
        // The key codes, not the keys: a parameter of a value class would be given to JUnit unboxed.
        @JvmStatic
        fun moves(): List<Arguments> = listOf(
            arguments("◀ (arrow or cross)", Key.DirectionLeft.keyCode, ControlButton.LEFT),
            arguments("▼ (arrow or cross)", Key.DirectionDown.keyCode, ControlButton.DOWN),
            arguments("▶ (arrow or cross)", Key.DirectionRight.keyCode, ControlButton.RIGHT),
            arguments("Q (ZQSD)", Key.Q.keyCode, ControlButton.LEFT),
            arguments("A (WASD)", Key.A.keyCode, ControlButton.LEFT),
            arguments("S", Key.S.keyCode, ControlButton.DOWN),
            arguments("D", Key.D.keyCode, ControlButton.RIGHT),
        )

        @JvmStatic
        fun clockwise(): List<Arguments> = listOf(
            arguments("▲ (arrow or cross)", Key.DirectionUp.keyCode),
            arguments("X", Key.X.keyCode),
            arguments("K", Key.K.keyCode),
            arguments("A of a pad", Key.ButtonA.keyCode),
        )

        @JvmStatic
        fun counterclockwise(): List<Arguments> = listOf(
            arguments("Z (ZQSD, or Z X in qwerty)", Key.Z.keyCode),
            arguments("W (WASD, or W X in azerty)", Key.W.keyCode),
            arguments("Y (Y X in qwertz)", Key.Y.keyCode),
            arguments("J", Key.J.keyCode),
            arguments("left Ctrl", Key.CtrlLeft.keyCode),
            arguments("right Ctrl", Key.CtrlRight.keyCode),
            arguments("B of a pad", Key.ButtonB.keyCode),
            arguments("X of a pad, left of A on a pad laid out as A B X Y", Key.ButtonX.keyCode),
        )

        @JvmStatic
        fun pauseKeys(): List<Arguments> = listOf(
            arguments("Escape", Key.Escape.keyCode),
            arguments("P", Key.P.keyCode),
            arguments("Start of a pad", Key.ButtonStart.keyCode),
        )

        @JvmStatic
        fun otherKeys(): List<Arguments> = listOf(
            arguments("the system back", Key.Back.keyCode),
            arguments("Tab, which moves the focus", Key.Tab.keyCode),
            arguments("the volume", Key.VolumeUp.keyCode),
            arguments("a letter of no use", Key.E.keyCode),
            arguments("a diagonal of a cross", Key.DirectionUpLeft.keyCode),
            arguments("another diagonal of a cross", Key.DirectionDownRight.keyCode),
        )
    }
}
