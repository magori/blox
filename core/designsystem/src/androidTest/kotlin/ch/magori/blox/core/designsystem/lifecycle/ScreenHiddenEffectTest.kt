package ch.magori.blox.core.designsystem.lifecycle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ScreenHiddenEffectTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a pause alone, behind a dialog or in a split screen, hides nothing")
    fun aPauseAloneHidesNothing() = extension.use {
        // Given a visible screen, in front
        val screen = TestScreen()
        var hidings = 0
        show(screen) { hidings++ }

        // When it is paused
        runOnIdle { screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        // Then nothing happens
        runOnIdle { assertThat(hidings).isZero() }
    }

    @Test
    @DisplayName("a screen no longer visible says so once each time it is hidden")
    fun aScreenNoLongerVisibleSaysSo() = extension.use {
        // Given a visible screen, in front
        val screen = TestScreen()
        var hidings = 0
        show(screen) { hidings++ }

        // When it is no longer visible (the application in the background)
        runOnIdle { screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }

        // Then it says so, once
        runOnIdle { assertThat(hidings).isEqualTo(1) }

        // And again when, back in front, it is hidden once more
        runOnIdle {
            screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        runOnIdle { assertThat(hidings).isEqualTo(2) }
    }

    @Test
    @DisplayName("a screen left says nothing more when its lifecycle stops afterwards")
    fun aScreenLeftSaysNothingMore() = extension.use {
        // Given a visible screen, then left: the effect leaves the composition
        val screen = TestScreen()
        var hidings = 0
        var shown by mutableStateOf(true)
        runOnUiThread { screen.lifecycle.currentState = Lifecycle.State.RESUMED }
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides screen) {
                if (shown) ScreenHiddenEffect { hidings++ }
            }
        }
        runOnIdle { shown = false }

        // When its lifecycle stops
        runOnIdle { screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }

        // Then nothing is said: the observer went with the screen
        runOnIdle { assertThat(hidings).isZero() }
    }

    private fun ComposeContext.show(
        screen: TestScreen,
        onHidden: () -> Unit,
    ) {
        runOnUiThread { screen.lifecycle.currentState = Lifecycle.State.RESUMED }
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides screen) {
                ScreenHiddenEffect(onHidden = onHidden)
            }
        }
    }

    /** A screen whose lifecycle the test drives. */
    private class TestScreen : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }
}
