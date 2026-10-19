package ch.magori.blox.feature.game.controls

import androidx.compose.ui.input.key.Key
import ch.magori.blox.feature.game.controls.ControlChange.Pressed
import ch.magori.blox.feature.game.controls.ControlChange.Released
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HeldButtonsTest {
    @Test
    fun `a finger put on a button sends a single press`() {
        // Given
        val held = HeldButtons()

        // When
        val step = held.press(finger = 1, ControlButton.LEFT)

        // Then
        assertThat(step.changes).containsExactly(Pressed(ControlButton.LEFT))
    }

    @Test
    fun `lifting that finger sends a release`() {
        // Given
        val held = HeldButtons().press(finger = 1, ControlButton.LEFT).held

        // When
        val step = held.lift(finger = 1)

        // Then
        assertThat(step.changes).containsExactly(Released(ControlButton.LEFT))
    }

    @Test
    fun `a second finger on another button does not release the first, so left can be held while pressing A`() {
        // Given left held by finger 1
        val holdingLeft = HeldButtons().press(finger = 1, ControlButton.LEFT).held

        // When finger 2 presses A, then lifts
        val pressA = holdingLeft.press(finger = 2, ControlButton.A)
        val liftA = pressA.held.lift(finger = 2)

        // Then only A changes, and left is released only when finger 1 lifts
        assertThat(pressA.changes).containsExactly(Pressed(ControlButton.A))
        assertThat(liftA.changes).containsExactly(Released(ControlButton.A))
        assertThat(liftA.held.lift(finger = 1).changes).containsExactly(Released(ControlButton.LEFT))
    }

    @Test
    fun `leaving the screen releases every held button`() {
        // Given left and A held
        val held = HeldButtons()
            .press(finger = 1, ControlButton.LEFT).held
            .press(finger = 2, ControlButton.A).held

        // When the controls leave the screen
        val step = held.cancel()

        // Then both are released, and nothing is held any more
        assertThat(step.changes).containsExactlyInAnyOrder(Released(ControlButton.LEFT), Released(ControlButton.A))
        assertThat(step.held.lift(finger = 1).changes).isEmpty()
        assertThat(step.held.lift(finger = 2).changes).isEmpty()
    }

    @Test
    fun `two fingers on the same button give a single press, and the button is released when the last one lifts`() {
        // Given finger 1 on A
        val first = HeldButtons().press(finger = 1, ControlButton.A)

        // When finger 2 also presses A, then finger 1 lifts, then finger 2
        val second = first.held.press(finger = 2, ControlButton.A)
        val firstLifts = second.held.lift(finger = 1)
        val lastLifts = firstLifts.held.lift(finger = 2)

        // Then
        assertThat(first.changes).containsExactly(Pressed(ControlButton.A))
        assertThat(second.changes).isEmpty()
        assertThat(firstLifts.changes).isEmpty()
        assertThat(lastLifts.changes).containsExactly(Released(ControlButton.A))
    }

    @Test
    fun `leaving the screen releases a button held by two fingers only once`() {
        // Given A held by two fingers
        val held = HeldButtons()
            .press(finger = 1, ControlButton.A).held
            .press(finger = 2, ControlButton.A).held

        // When
        val step = held.cancel()

        // Then
        assertThat(step.changes).containsExactly(Released(ControlButton.A))
    }

    @Test
    fun `the held buttons are the buttons under at least one finger`() {
        // Given left under finger 1, A under fingers 2 and 3
        val held = HeldButtons()
            .press(finger = 1, ControlButton.LEFT).held
            .press(finger = 2, ControlButton.A).held
            .press(finger = 3, ControlButton.A).held

        // When finger 1 lifts
        val afterLift = held.lift(finger = 1).held

        // Then
        assertThat(held.heldButtons).containsExactlyInAnyOrder(ControlButton.LEFT, ControlButton.A)
        assertThat(afterLift.heldButtons).containsExactly(ControlButton.A)
        assertThat(HeldButtons().heldButtons).isEmpty()
    }

    @Test
    fun `pressing again with a known finger releases its previous button`() {
        // Given finger 1 on left
        val held = HeldButtons().press(finger = 1, ControlButton.LEFT).held

        // When the same finger presses down, without having lifted
        val step = held.press(finger = 1, ControlButton.DOWN)

        // Then
        assertThat(step.changes).containsExactly(Released(ControlButton.LEFT), Pressed(ControlButton.DOWN))
        assertThat(step.held.heldButtons).containsExactly(ControlButton.DOWN)
    }

    @Test
    fun `a tap from accessibility services sends a press then a release, and leaves nothing held`() {
        // When
        val step = HeldButtons().tap(ControlButton.A)

        // Then
        assertThat(step.changes).containsExactly(Pressed(ControlButton.A), Released(ControlButton.A))
        assertThat(step.held.heldButtons).isEmpty()
    }

    @Test
    fun `a key holds its button as a finger does, pressed going down and released coming back up`() {
        // When
        val down = HeldButtons().pressKey(Key.DirectionLeft, ControlButton.LEFT)
        val up = down.held.liftKey(Key.DirectionLeft)

        // Then
        assertThat(down.changes).containsExactly(Pressed(ControlButton.LEFT))
        assertThat(down.held.heldButtons).containsExactly(ControlButton.LEFT)
        assertThat(up.changes).containsExactly(Released(ControlButton.LEFT))
    }

    @Test
    fun `a key and a finger are never taken for each other, even when the key code is the pointer id`() {
        // Given finger 1 on left
        val held = HeldButtons().press(finger = 1, ControlButton.LEFT).held

        // When the key of code 1 goes down on A, then back up
        val down = held.pressKey(Key(keyCode = 1), ControlButton.A)
        val up = down.held.liftKey(Key(keyCode = 1))

        // Then left stays held by the finger
        assertThat(down.changes).containsExactly(Pressed(ControlButton.A))
        assertThat(down.held.heldButtons).containsExactlyInAnyOrder(ControlButton.LEFT, ControlButton.A)
        assertThat(up.changes).containsExactly(Released(ControlButton.A))
        assertThat(up.held.heldButtons).containsExactly(ControlButton.LEFT)
    }

    @Test
    fun `a finger and a key on the same button press it once, and release it when the last one lets go`() {
        // Given a finger on left
        val touched = HeldButtons().press(finger = 1, ControlButton.LEFT)

        // When the left arrow also goes down, then the finger lifts, then the arrow comes back up
        val keyDown = touched.held.pressKey(Key.DirectionLeft, ControlButton.LEFT)
        val fingerLifts = keyDown.held.lift(finger = 1)
        val keyUp = fingerLifts.held.liftKey(Key.DirectionLeft)

        // Then
        assertThat(keyDown.changes).isEmpty()
        assertThat(fingerLifts.changes).isEmpty()
        assertThat(keyUp.changes).containsExactly(Released(ControlButton.LEFT))
    }
}
