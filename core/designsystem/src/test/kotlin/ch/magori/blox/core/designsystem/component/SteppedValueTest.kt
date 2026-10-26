package ch.magori.blox.core.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class SteppedValueTest {
    @Test
    fun `the nearest step to a position on the track is the step closest to it`() {
        // Given a volume from 0 to 100 by steps of 10
        val volume = SteppedValue(value = 50, steps = 0..100 step 10)

        // Then a third of the way gives 30, a little more than a third 40
        assertThat(volume.nearestTo(0.34f).value).isEqualTo(30)
        assertThat(volume.nearestTo(0.36f).value).isEqualTo(40)
    }

    @Test
    fun `a position before the track or past it gives the first or the last step`() {
        // Given a level from 0 to 9, a finger dragged off the gauge on either side
        val level = SteppedValue(value = 5, steps = 0..9)

        // Then the value stays in its range
        assertThat(level.nearestTo(-0.5f).value).isEqualTo(0)
        assertThat(level.nearestTo(1.7f).value).isEqualTo(9)
    }

    @Test
    fun `a value asked between two steps or out of their range gives the nearest step`() {
        // Given a volume, and values as TalkBack or a keyboard may ask for them
        val volume = SteppedValue(value = 50, steps = 0..100 step 10)

        // Then
        assertThat(volume.nearestToValue(63f).value).isEqualTo(60)
        assertThat(volume.nearestToValue(67f).value).isEqualTo(70)
        assertThat(volume.nearestToValue(250f).value).isEqualTo(100)
        assertThat(volume.nearestToValue(-3f).value).isEqualTo(0)
    }

    @Test
    fun `one step up or down moves by one step, never past the first or the last`() {
        // Given
        val steps = 0..100 step 10

        // Then
        assertThat(SteppedValue(50, steps).increased().value).isEqualTo(60)
        assertThat(SteppedValue(50, steps).decreased().value).isEqualTo(40)
        assertThat(SteppedValue(100, steps).increased().value).isEqualTo(100)
        assertThat(SteppedValue(0, steps).decreased().value).isEqualTo(0)
    }

    @Test
    fun `a value can go down but at its first step and up but at its last`() {
        // Given
        val steps = 0..9

        // Then
        assertThat(SteppedValue(0, steps)).extracting({
            it.canDecrease
        }, { it.canIncrease }).containsExactly(false, true)
        assertThat(SteppedValue(5, steps)).extracting({
            it.canDecrease
        }, { it.canIncrease }).containsExactly(true, true)
        assertThat(SteppedValue(9, steps)).extracting({
            it.canDecrease
        }, { it.canIncrease }).containsExactly(true, false)
    }

    @Test
    fun `the position of a value goes from 0 at the first step to 1 at the last`() {
        // Given
        val steps = 0..100 step 10

        // Then
        assertThat(SteppedValue(0, steps).fraction).isZero()
        assertThat(SteppedValue(30, steps).fraction).isEqualTo(0.3f)
        assertThat(SteppedValue(100, steps).fraction).isEqualTo(1f)
    }

    @Test
    fun `a single step stands at the start of the track`() {
        assertThat(SteppedValue(4, 4..4).fraction).isZero()
    }

    @ParameterizedTest
    @ValueSource(ints = [-10, 45, 110])
    fun `a value off its steps or out of their range is refused`(value: Int) {
        assertThatIllegalArgumentException().isThrownBy { SteppedValue(value = value, steps = 0..100 step 10) }
    }

    @Test
    fun `a value is checked against its steps by arithmetic, without going through them`() {
        // Given a billion steps: going through them one by one, boxed, would take seconds
        val steps = 0..2_000_000_000 step 2

        // Then the last one is accepted at once
        assertTimeoutPreemptively(Duration.ofMillis(200)) {
            assertThat(SteppedValue(2_000_000_000, steps).index).isEqualTo(1_000_000_000)
        }
    }

    @Test
    fun `steps that go down are refused`() {
        assertThatIllegalArgumentException().isThrownBy { SteppedValue(value = 5, steps = 9 downTo 0) }
    }

    @Test
    fun `a position or a value that is not a number leaves the value as it is`() {
        // Given a volume, and what a faulty accessibility service could ask for
        val volume = SteppedValue(value = 50, steps = 0..100 step 10)

        // Then
        assertThat(volume.nearestToValue(Float.NaN)).isEqualTo(volume)
        assertThat(volume.nearestTo(Float.NaN)).isEqualTo(volume)
    }
}
