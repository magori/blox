package ch.magori.blox.feature.game.controls

/** The buttons of the game screen. The ViewModel turns them into inputs of the engine. */
internal enum class ControlButton { LEFT, DOWN, RIGHT, A, B }

/** What the screen reports: a button goes down, or comes back up. */
internal sealed interface ControlChange {
    val button: ControlButton

    data class Pressed(override val button: ControlButton) : ControlChange

    data class Released(override val button: ControlButton) : ControlChange
}
