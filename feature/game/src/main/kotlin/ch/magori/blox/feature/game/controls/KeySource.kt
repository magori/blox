package ch.magori.blox.feature.game.controls

import android.view.InputDevice

/** Where a key event comes from: the up of the cross of a pad does nothing, the up arrow of a keyboard turns. */
internal enum class KeySource { KEYBOARD, PAD }

/**
 * The source of a key event whose [inputSource] is that of Android (`KeyEvent.getSource`): a pad when it
 * comes from a gamepad or a joystick, even one that also says it is a keyboard, as many pads do.
 */
internal fun keySourceOf(inputSource: Int): KeySource {
    val pad = listOf(InputDevice.SOURCE_GAMEPAD, InputDevice.SOURCE_JOYSTICK).any { inputSource and it == it }
    return if (pad) KeySource.PAD else KeySource.KEYBOARD
}
