package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.rules.RepeatTiming

/**
 * An input held over several ticks, such as a shift (DAS) or a soft drop: it says, on each tick,
 * whether a step is due. It knows nothing of the board: the engine applies the step.
 *
 * Immutable: [tick] gives the state for the next tick. A release resets it, so the next press
 * starts again from tick 0. Its state can only come from [tick], never be forged.
 */
@ConsistentCopyVisibility
public data class AutoRepeat private constructor(private val timing: RepeatTiming, private val ticksHeld: Int?) {
    /** An input not held yet, repeating with [timing]. */
    public constructor(timing: RepeatTiming) : this(timing, null)

    /**
     * The repeat charged after a step the board refused, such as a shift against a wall: the next
     * held tick gives a step at once, then the repeat goes on every [RepeatTiming.interval] ticks.
     * The engine calls it after each refused step, so the piece moves as soon as the way is free.
     * The ROM does the same by setting its DAS counter to 16 (`$8A05` / `$8A07`).
     */
    public fun charged(): AutoRepeat = copy(ticksHeld = timing.firstRepeat - 1)

    /** Advances by one tick, where the input is [pressed] or not. */
    public fun tick(pressed: Boolean): AutoRepeatTick {
        if (!pressed) return AutoRepeatTick(next = copy(ticksHeld = null), step = false)
        val tick = ticksHeld?.plus(1) ?: 0
        return AutoRepeatTick(next = copy(ticksHeld = tick), step = timing.stepsOn(tick))
    }
}

/** The result of one tick of an [AutoRepeat]: its [next] state, and whether a [step] is due on that tick. */
public data class AutoRepeatTick(public val next: AutoRepeat, public val step: Boolean)
