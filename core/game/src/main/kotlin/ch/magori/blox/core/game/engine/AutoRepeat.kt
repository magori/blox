package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.rules.RepeatTiming

/**
 * An input held over several ticks, such as a shift (DAS) or a soft drop: it says, on each tick,
 * whether a step is due. It knows nothing of the board: the engine applies the step.
 *
 * Immutable: [tick] gives the state for the next tick. A release resets it, so the next press
 * starts again from tick 0. Its state can only come from the secondary constructor, [tick], [charged] or
 * [heldWithoutPress], never be forged.
 */
@ConsistentCopyVisibility
internal data class AutoRepeat private constructor(private val timing: RepeatTiming, private val ticksHeld: Int?) {
    /** An input not held yet, repeating with [timing]. */
    constructor(timing: RepeatTiming) : this(timing, null)

    /**
     * The repeat charged after a step the board refused, such as a shift against a wall: the next
     * held tick gives a step at once, then the repeat goes on every [RepeatTiming.interval] ticks.
     * The engine calls it after each refused step, so the piece moves as soon as the way is free.
     * The ROM does the same by setting its DAS counter to 16 (`$8A05` / `$8A07`).
     */
    fun charged(): AutoRepeat = copy(ticksHeld = timing.firstRepeat.value - 1)

    /** Advances by one tick, where the input is [pressed] or not. */
    fun tick(pressed: Boolean): AutoRepeatTick {
        if (!pressed) return AutoRepeatTick(next = copy(ticksHeld = null), step = false)
        val tick = ticksHeld?.plus(1) ?: 0
        return AutoRepeatTick(next = copy(ticksHeld = tick), step = timing.stepsOn(tick))
    }

    companion object {
        /**
         * The repeat of an input found held with no press seen and no count yet, such as left pressed during
         * the entry delay before any shift of the game: its count stands at 0, as just after a press, but
         * without the step of the press. The first held tick counts 1, so the first step comes on the
         * [RepeatTiming.firstRepeat]-th held tick (the 16th for the Classic shift), then every
         * [RepeatTiming.interval] ticks.
         *
         * It stands for the DAS counter of the ROM, `autorepeatX` (`$0046`), at 0 (meatfighter.com,
         * "Applying AI to Nintendo Tetris"): only a new press of left or right sets it to 0 and shifts
         * (`$89B8`, `$89D3`); otherwise a held left or right increments it (`$89C4`) and shifts once it
         * reaches 16 (`$89C6` to `$89CE`). The routine is not called during the line clear or the entry
         * delay, so a press made then is lost and the held button goes on with the counter.
         *
         * Not confirmed: the source does not say what `autorepeatX` holds before the first shift of a game
         * (power-on, reset or game start); 0 supposes a cleared RAM. Once a shift has run, the engine keeps
         * its repeat instead, as the ROM keeps its counter.
         */
        fun heldWithoutPress(timing: RepeatTiming): AutoRepeat = AutoRepeat(timing, ticksHeld = 0)
    }
}

/** The result of one tick of an [AutoRepeat]: its [next] state, and whether a [step] is due on that tick. */
internal data class AutoRepeatTick(val next: AutoRepeat, val step: Boolean)
