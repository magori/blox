package ch.magori.blox.theme

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SystemAnimationsTest {
    @ParameterizedTest(name = "animating {0}")
    @ValueSource(booleans = [true, false])
    fun `whether Android animates is known at once`(animating: Boolean) = runTest {
        // Given Android animating or not as the application starts
        val setting = FakeAnimationSetting(animating)

        // When
        val animations = SystemAnimations(setting, backgroundScope)

        // Then
        assertThat(animations.animate.value).isEqualTo(animating)
    }

    @Test
    fun `removing the animations while the application runs is seen at once, and so is giving them back`() = runTest {
        // Given Android animating, as the application runs (in split screen, it is never paused)
        val setting = FakeAnimationSetting(animating = true)
        val animations = SystemAnimations(setting, backgroundScope)

        animations.animate.test {
            assertThat(awaitItem()).isTrue()

            // When the player removes the animations, then gives them back
            setting.change(animating = false)
            val removed = awaitItem()
            setting.change(animating = true)

            // Then
            assertThat(removed).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }
}

/** A setting of the animations that the test changes, telling of each change as Android does. */
private class FakeAnimationSetting(private var animating: Boolean) : AnimationSetting {
    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val changes: Flow<Unit> = changed

    override fun animates(): Boolean = animating

    fun change(animating: Boolean) {
        this.animating = animating
        changed.tryEmit(Unit)
    }
}
