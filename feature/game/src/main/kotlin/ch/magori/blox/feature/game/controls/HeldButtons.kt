package ch.magori.blox.feature.game.controls

/**
 * Which finger holds which button. Immutable: each event gives a [Step], the new state and the
 * changes to report. A button is pressed when its first finger lands and released when its last
 * finger lifts, so two fingers on one button press it once; fingers on different buttons are
 * independent (left held while A is pressed). Fingers are the pointer ids of Compose.
 */
internal data class HeldButtons(private val fingers: Map<Long, ControlButton> = emptyMap()) {
    /** The buttons under at least one finger, to draw them pressed. */
    val heldButtons: Set<ControlButton> = fingers.values.toSet()

    /** [finger] lands on [button]. A finger already down first leaves its previous button. */
    fun press(
        finger: Long,
        button: ControlButton,
    ): Step {
        val (withoutFinger, released) = lift(finger)
        val pressed = if (withoutFinger.isHeld(button)) emptyList() else listOf(ControlChange.Pressed(button))
        return Step(HeldButtons(withoutFinger.fingers + (finger to button)), released + pressed)
    }

    /** [finger] lifts; an unknown finger changes nothing. */
    fun lift(finger: Long): Step {
        val button = fingers[finger] ?: return Step(this, emptyList())
        val remaining = HeldButtons(fingers - finger)
        val changes = if (remaining.isHeld(button)) emptyList() else listOf(ControlChange.Released(button))
        return Step(remaining, changes)
    }

    /** A click from an accessibility service (TalkBack): [button] goes down and up at once. */
    fun tap(button: ControlButton): Step =
        Step(this, listOf(ControlChange.Pressed(button), ControlChange.Released(button)))

    /**
     * The controls leave the screen: every held button is released. An interruption by the system
     * arrives as fingers lifting, handled by [lift].
     */
    fun cancel(): Step = Step(HeldButtons(), fingers.values.distinct().map { ControlChange.Released(it) })

    private fun isHeld(button: ControlButton): Boolean = button in heldButtons

    data class Step(val held: HeldButtons, val changes: List<ControlChange>)
}
