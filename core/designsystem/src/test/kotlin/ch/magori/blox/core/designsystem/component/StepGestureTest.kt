package ch.magori.blox.core.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StepGestureTest {
    private val steps = 0..100 step 10

    private fun at(value: Int) = SteppedValue(value, steps)

    @Test
    fun `while the value comes back late, the next arrow goes on from the last step asked`() {
        // Given 60 then 70 asked for by two arrows from 50
        val gesture = StepGesture()
        gesture.keyAsked(at(50), at(60))
        gesture.keyAsked(at(50), at(70))

        // When the value comes back at 60, on its way to 70
        val from = gesture.fromKeys(at(60))

        // Then the next arrow goes on from 70
        assertThat(from).isEqualTo(at(70))
    }

    @Test
    fun `once the value reaches the step asked, or leaves the way to it, the step asked is forgotten`() {
        // Given 70 asked for from 50, the value come back at 70
        val gesture = StepGesture()
        gesture.keyAsked(at(50), at(70))
        assertThat(gesture.fromKeys(at(70))).isEqualTo(at(70))

        // When the value then moves, by another hand, back to 60
        // Then the arrows go on from it, the step asked for being forgotten
        assertThat(gesture.fromKeys(at(60))).isEqualTo(at(60))

        // And a step asked for from 50 is forgotten when the value leaves the way to it
        gesture.keyAsked(at(50), at(70))
        assertThat(gesture.fromKeys(at(30))).isEqualTo(at(30))
        assertThat(gesture.fromKeys(at(60))).isEqualTo(at(60))
    }
}
