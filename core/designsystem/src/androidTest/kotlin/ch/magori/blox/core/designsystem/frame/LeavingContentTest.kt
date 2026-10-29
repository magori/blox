package ch.magori.blox.core.designsystem.frame

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class LeavingContentTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private var open by mutableStateOf<String?>(null)

    @Test
    @DisplayName("closed, it leaves on its last value for its length, then is gone")
    fun closedItLeavesForItsLengthThenIsGone() = extension.use {
        // Given « A » open
        show(length = LENGTH)

        // When it closes
        runOnIdle { open = null }
        mainClock.advanceTimeByFrame()

        // Then it leaves on « A », then is gone
        onNodeWithText("A leaving").assertExists()
        mainClock.advanceTimeBy(LONGER_MS)
        onNodeWithText("A leaving").assertDoesNotExist()
    }

    @Test
    @DisplayName("closed, then open again as it leaves, it shows the value open, no longer leaving, and stays")
    fun openAgainAsItLeavesItShowsTheValueOpen() = extension.use {
        // Given « A » leaving
        show(length = LENGTH)
        runOnIdle { open = null }
        mainClock.advanceTimeByFrame()

        // When « B » opens before it has gone
        runOnIdle { open = "B" }
        mainClock.advanceTimeByFrame()

        // Then
        onNodeWithText("B open").assertExists()
        onNodeWithText("A leaving").assertDoesNotExist()
        mainClock.advanceTimeBy(LONGER_MS)
        onNodeWithText("B open").assertExists()
    }

    @Test
    @DisplayName("of a length of 0, closed, it is gone at once")
    fun ofALengthOf0ItIsGoneAtOnce() = extension.use {
        // Given « A » open, to leave in no time
        show(length = 0)

        // When
        runOnIdle { open = null }
        mainClock.advanceTimeByFrame()

        // Then
        onNodeWithText("A", substring = true).assertDoesNotExist()
    }

    @Test
    @DisplayName("never open, it composes nothing")
    fun neverOpenItComposesNothing() = extension.use {
        // Given / When nothing open since it is composed
        var composed = 0
        mainClock.autoAdvance = false
        setContent {
            LeavingContent(open = null as String?, length = LENGTH) { _, _ -> composed++ }
        }
        mainClock.advanceTimeBy(LONGER_MS)

        // Then
        runOnIdle { assertThat(composed).isZero() }
    }

    /** « A » open, its content telling its value and whether it leaves, on a clock that only moves when told. */
    private fun ComposeContext.show(length: Int) {
        open = "A"
        mainClock.autoAdvance = false
        setContent {
            LeavingContent(open, length) { value, leaving ->
                Text(if (leaving == null) "$value open" else "$value leaving")
            }
        }
        mainClock.advanceTimeByFrame()
        onNodeWithText("A open").assertExists()
    }

    private companion object {
        /** 300 ms, and longer than that. */
        const val LENGTH = 18
        const val LONGER_MS = 1_000L
    }
}
