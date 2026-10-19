package ch.magori.blox.core.game.engine

/**
 * The state of the controller, as the engine reads it on each tick. Only the engine changes it, from the
 * [Input]s given to [GameEngine.apply] and on each tick.
 *
 * It knows the buttons held, the buttons newly pressed since the last tick (even if released since, so that
 * a short tap between two ticks still counts), and the repeat of the shift. As in the ROM, there is a single
 * shift repeat for left and right, which only a new press of left or right starts again.
 */
internal data class Controls(val held: Set<Button>, val pressedSinceLastTick: Set<Button>, val shift: AutoRepeat?) {
    /** After a press of [button]: if it was not held yet, it is held, and counts as a new press until the next tick. */
    fun afterPress(button: Button): Controls =
        if (button in held) this else copy(held = held + button, pressedSinceLastTick = pressedSinceLastTick + button)

    /** After a release of [button]. */
    fun afterRelease(button: Button): Controls = copy(held = held - button)

    /** Once a tick has read the new presses. */
    fun afterTick(): Controls = copy(pressedSinceLastTick = emptySet())

    companion object {
        /** No button held. */
        val NONE: Controls = Controls(held = emptySet(), pressedSinceLastTick = emptySet(), shift = null)
    }
}
