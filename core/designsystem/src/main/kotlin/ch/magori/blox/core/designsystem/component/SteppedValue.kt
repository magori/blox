package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * A [value] that moves by [steps]: from their first to their last, by their step (`0..100 step 10` for a volume,
 * `0..9` for a level), going up. What a slider or a gauge shows and changes; it knows nothing of what the value means.
 * The steps are checked by arithmetic, not gone through: a value off them, or steps that go down, are refused.
 */
@Immutable
internal data class SteppedValue(val value: Int, val steps: IntProgression) {
    init {
        require(steps.step > 0) { "Steps go up: $steps" }
        require(value in steps.first..steps.last && (value - steps.first) % steps.step == 0) {
            "$value is not one of the steps $steps"
        }
    }

    /** Which step this value is, from 0 at the first step. */
    val index: Int get() = (value - steps.first) / steps.step

    /** The index of the last step: one less than the number of steps. */
    val lastIndex: Int get() = (steps.last - steps.first) / steps.step

    /** How far along the track this value stands: 0 at the first step, 1 at the last; 0 if there is only one. */
    val fraction: Float get() = fractionOf(index)

    /** How far along the track the step of [index] stands: 0 at the first step, 1 at the last; 0 if there is only one. */
    fun fractionOf(index: Int): Float = if (lastIndex == 0) 0f else index.toFloat() / lastIndex

    /** Whether there is a step below this one. */
    val canDecrease: Boolean get() = index > 0

    /** Whether there is a step above this one. */
    val canIncrease: Boolean get() = index < lastIndex

    /**
     * The step nearest to [fraction] of the way along the track, from 0 at the first step to 1 at the last; this one if
     * [fraction] is not a number.
     */
    fun nearestTo(fraction: Float): SteppedValue =
        if (fraction.isNaN()) this else atIndex((fraction * lastIndex).roundToInt())

    /**
     * The step nearest to [target], a value between the steps or out of their range (TalkBack, a keyboard); this one if
     * [target] is not a number, as a faulty accessibility service may ask.
     */
    fun nearestToValue(target: Float): SteppedValue =
        if (target.isNaN()) this else atIndex(((target - steps.first) / steps.step).roundToInt())

    /** One step up, or the last step again. */
    fun increased(): SteppedValue = atIndex(index + 1)

    /** One step down, or the first step again. */
    fun decreased(): SteppedValue = atIndex(index - 1)

    /** The step of [index], brought back to the first or the last if it is beyond them. */
    private fun atIndex(index: Int): SteppedValue =
        copy(value = steps.first + index.coerceIn(0, lastIndex) * steps.step)
}
