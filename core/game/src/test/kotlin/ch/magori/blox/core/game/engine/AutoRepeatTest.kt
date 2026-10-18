package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.rules.ClassicInputRepeat
import ch.magori.blox.core.game.rules.InputRepeat
import ch.magori.blox.core.game.rules.RepeatTiming
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AutoRepeatTest {
    private val inputs: InputRepeat = ClassicInputRepeat

    @Test
    fun `on the first press the piece shifts at once`() {
        assertThat(ticksWithAStep(inputs.shift, held(ticks = 1))).containsExactly(0)
    }

    @Test
    fun `while the shift is held nothing happens for 15 ticks then the piece shifts 16 ticks after the press`() {
        assertThat(ticksWithAStep(inputs.shift, held(ticks = 17))).containsExactly(0, 16)
    }

    @Test
    fun `then the held shift moves the piece every 6 ticks`() {
        assertThat(ticksWithAStep(inputs.shift, held(ticks = 35))).containsExactly(0, 16, 22, 28, 34)
    }

    @Test
    fun `after a release a new press shifts at once and the count starts again`() {
        val pressed = held(ticks = 10) + released(ticks = 1) + held(ticks = 17)

        assertThat(ticksWithAStep(inputs.shift, pressed)).containsExactly(0, 11, 27)
    }

    @Test
    fun `a held soft drop moves the piece down a first row 2 ticks after the press then every 2 ticks`() {
        assertThat(ticksWithAStep(inputs.softDrop, held(ticks = 9))).containsExactly(2, 4, 6, 8)
    }

    @Test
    fun `releasing the soft drop stops it at once`() {
        val pressed = held(ticks = 4) + released(ticks = 4)

        assertThat(ticksWithAStep(inputs.softDrop, pressed)).containsExactly(2)
    }

    @Test
    fun `a shift charged against a wall moves at once when the way is free, then every 6 ticks`() {
        val shiftRefusedByAWall = AutoRepeat(inputs.shift).tick(pressed = true).next.charged()

        assertThat(ticksWithAStep(shiftRefusedByAWall, held(ticks = 13))).containsExactly(0, 6, 12)
    }

    private fun held(ticks: Int): List<Boolean> = List(ticks) { true }

    private fun released(ticks: Int): List<Boolean> = List(ticks) { false }

    /** When: from a fresh press with [timing], the input is pressed or not on each tick; the ticks where a step is due. */
    private fun ticksWithAStep(
        timing: RepeatTiming,
        pressedPerTick: List<Boolean>,
    ): List<Int> = ticksWithAStep(AutoRepeat(timing), pressedPerTick)

    /** When: from [start], the input is pressed or not on each tick, in turn; the ticks where a step is due. */
    private fun ticksWithAStep(
        start: AutoRepeat,
        pressedPerTick: List<Boolean>,
    ): List<Int> {
        var repeat = start
        return pressedPerTick.mapIndexedNotNull { index, pressed ->
            val tick = repeat.tick(pressed)
            repeat = tick.next
            index.takeIf { tick.step }
        }
    }
}
