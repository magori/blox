package ch.magori.blox.feature.game.controls

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType

/** What a key event does in the game under way: see [keyAction]. */
internal sealed interface KeyAction {
    /** The key goes down: it holds [button], as a finger would. */
    data class Press(val button: ControlButton) : KeyAction

    /** The key comes back up: it lets go of the button it holds. */
    data object Release : KeyAction

    /** The game pauses. */
    data object Pause : KeyAction

    /** A key of the game with nothing to do (a repeat, the release of a pause key): kept from the system. */
    data object Ignored : KeyAction
}

/** What a key of the game stands for. */
private sealed interface Binding {
    data class Hold(val button: ControlButton) : Binding

    data object Pause : Binding
}

/**
 * The keys of the game. A keyboard sends the code of the character its layout gives the key, so ZQSD
 * (azerty) and WASD (qwerty) both work, the layout deciding which is under the left hand; the arrows and
 * the cross of a pad send the same codes. Z, W and Y are the counterclockwise rotation (B): the key up of
 * ZQSD or WASD, and the key left of X under the arrows (Z X in qwerty, W X in azerty, Y X in qwertz), as in
 * the emulators. J and K are B and A for the right hand of a player on ZQSD or WASD, in the order of the
 * pad. On a pad, X is B too: on the pads laid out as A B X Y, X is left of A, as B is on the NES. The
 * diagonals of a cross (`DPAD_UP_LEFT`…) are not keys of the game.
 */
private val bindings: Map<Key, Binding> = buildMap {
    fun bind(
        binding: Binding,
        keys: List<Key>,
    ) = keys.forEach { put(it, binding) }
    bind(Binding.Hold(ControlButton.LEFT), listOf(Key.DirectionLeft, Key.Q, Key.A))
    bind(Binding.Hold(ControlButton.DOWN), listOf(Key.DirectionDown, Key.S))
    bind(Binding.Hold(ControlButton.RIGHT), listOf(Key.DirectionRight, Key.D))
    bind(Binding.Hold(ControlButton.A), listOf(Key.DirectionUp, Key.X, Key.K, Key.ButtonA))
    bind(
        Binding.Hold(ControlButton.B),
        listOf(Key.Z, Key.W, Key.Y, Key.J, Key.CtrlLeft, Key.CtrlRight, Key.ButtonB, Key.ButtonX),
    )
    bind(Binding.Pause, listOf(Key.Escape, Key.P, Key.ButtonStart))
}

/**
 * What [key] does in the game under way, going down or coming back up ([type]), [repeatCount] being the
 * number of repeats of the keyboard while it is held. A key of the game holds its button from down to
 * up, as a finger does, and only its first down counts: the engine repeats a held button itself (DAS),
 * so the repeats of the keyboard are kept from it. Null for a key that is not of the game, left to the
 * system (back, Tab, volume). The up of the cross of a pad does nothing, as on the NES, where the up arrow
 * of a keyboard turns ([source]).
 */
internal fun keyAction(
    key: Key,
    type: KeyEventType,
    repeatCount: Int,
    source: KeySource,
): KeyAction? {
    val binding = bindings[key] ?: return null
    if (source == KeySource.PAD && key == Key.DirectionUp) return KeyAction.Ignored
    val firstDown = type == KeyEventType.KeyDown && repeatCount == 0
    return when (binding) {
        is Binding.Hold if type == KeyEventType.KeyUp -> KeyAction.Release
        is Binding.Hold if firstDown -> KeyAction.Press(binding.button)
        Binding.Pause if firstDown -> KeyAction.Pause
        else -> KeyAction.Ignored
    }
}

/** Whether [key] pauses the game under way. */
internal fun isPauseKey(key: Key): Boolean = bindings[key] == Binding.Pause

/** Whether [key] is a key of the game under way. */
internal fun isGameKey(key: Key): Boolean = key in bindings
