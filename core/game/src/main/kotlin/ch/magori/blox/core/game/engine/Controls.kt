package ch.magori.blox.core.game.engine

/**
 * The state of the controller, as the engine reads it on each tick. Only the engine changes it, from the
 * [Input]s given to [GameEngine.apply] and on each tick.
 *
 * It knows the buttons held, the buttons newly pressed since the last tick (even if released since, so that
 * a short tap between two ticks still counts), and the repeat of the shift. As in the ROM, there is a single
 * shift repeat for left and right, which only a new press of left or right starts again.
 */
@ConsistentCopyVisibility
public data class Controls internal constructor(
    internal val held: Set<Button>,
    internal val pressedSinceLastTick: Set<Button>,
    internal val shift: AutoRepeat?,
) {
    /** After [input]: a press of a button not held yet holds it, and counts as a new press until the next tick. */
    internal fun after(input: Input): Controls = when (input) {
        is Input.Press ->
            if (input.button in held) {
                this
            } else {
                copy(held = held + input.button, pressedSinceLastTick = pressedSinceLastTick + input.button)
            }

        is Input.Release -> copy(held = held - input.button)
    }

    /** Once a tick has read the new presses. */
    internal fun afterTick(): Controls = copy(pressedSinceLastTick = emptySet())

    internal companion object {
        /** No button held. */
        val NONE: Controls = Controls(held = emptySet(), pressedSinceLastTick = emptySet(), shift = null)
    }
}
