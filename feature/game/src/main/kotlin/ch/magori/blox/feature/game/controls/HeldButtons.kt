package ch.magori.blox.feature.game.controls

import androidx.compose.ui.input.key.Key

/**
 * Which finger or key holds which button. Immutable: each event gives a [Step], the new state and the
 * changes to report. A button is pressed when its first holder takes it and released when its last
 * holder lets go, so two fingers on one button, or a finger and a key, press it once; holders of
 * different buttons are independent (left held while A is pressed). Fingers are the pointer ids of
 * Compose, keys those of a keyboard or a pad: a key is a finger that cannot slide to another button.
 */
internal data class HeldButtons(private val holders: Map<Holder, ControlButton> = emptyMap()) {
    /** The buttons held by at least one finger or key, to draw them pressed. */
    val heldButtons: Set<ControlButton> = holders.values.toSet()

    /** [finger] lands on [button]. A finger already down first leaves its previous button. */
    fun press(
        finger: Long,
        button: ControlButton,
    ): Step = take(Holder.Finger(finger), button)

    /** [finger] lifts; an unknown finger changes nothing. */
    fun lift(finger: Long): Step = letGo(Holder.Finger(finger))

    /** [key] goes down, holding [button]. */
    fun pressKey(
        key: Key,
        button: ControlButton,
    ): Step = take(Holder.KeyDown(key), button)

    /** [key] comes back up; a key that was not down changes nothing. */
    fun liftKey(key: Key): Step = letGo(Holder.KeyDown(key))

    /** A click from an accessibility service (TalkBack): [button] goes down and up at once. */
    fun tap(button: ControlButton): Step =
        Step(this, listOf(ControlChange.Pressed(button), ControlChange.Released(button)))

    /**
     * The controls leave the screen: every held button is released. An interruption by the system
     * arrives as fingers lifting, handled by [lift].
     */
    fun cancel(): Step = Step(HeldButtons(), holders.values.distinct().map { ControlChange.Released(it) })

    private fun take(
        holder: Holder,
        button: ControlButton,
    ): Step {
        val (withoutHolder, released) = letGo(holder)
        val pressed = if (withoutHolder.isHeld(button)) emptyList() else listOf(ControlChange.Pressed(button))
        return Step(HeldButtons(withoutHolder.holders + (holder to button)), released + pressed)
    }

    private fun letGo(holder: Holder): Step {
        val button = holders[holder] ?: return Step(this, emptyList())
        val remaining = HeldButtons(holders - holder)
        val changes = if (remaining.isHeld(button)) emptyList() else listOf(ControlChange.Released(button))
        return Step(remaining, changes)
    }

    private fun isHeld(button: ControlButton): Boolean = button in heldButtons

    data class Step(val held: HeldButtons, val changes: List<ControlChange>)

    /** What holds a button: a finger, or a key, never taken for each other. */
    sealed interface Holder {
        data class Finger(val id: Long) : Holder

        data class KeyDown(val key: Key) : Holder
    }
}
