package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.platform.AccessibilityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MessageTimeoutTest {
    @Test
    fun `without accessibility settings a message stays 4 seconds, and 10 with an action`() {
        // When
        val alone = messageTimeoutMillis(hasAction = false, accessibilityManager = null)
        val withAction = messageTimeoutMillis(hasAction = true, accessibilityManager = null)

        // Then
        assertThat(alone).isEqualTo(4_000L)
        assertThat(withAction).isEqualTo(10_000L)
    }

    @Test
    fun `a message stays as long as Android recommends, told that it bears text, a cross and its action`() {
        // Given a player who asked Android for more time to act (WCAG 2.1, 2.2.1)
        val asked = mutableListOf<Asked>()
        val accessibility = object : AccessibilityManager {
            override fun calculateRecommendedTimeoutMillis(
                originalTimeoutMillis: Long,
                containsIcons: Boolean,
                containsText: Boolean,
                containsControls: Boolean,
            ): Long {
                asked += Asked(originalTimeoutMillis, containsIcons, containsText, containsControls)
                return if (containsControls) 60_000L else originalTimeoutMillis
            }
        }

        // When
        val timeout = messageTimeoutMillis(hasAction = true, accessibilityManager = accessibility)

        // Then
        assertThat(timeout).isEqualTo(60_000L)
        assertThat(asked).containsExactly(Asked(10_000L, icons = true, text = true, controls = true))
    }

    /** What Android was asked about a message. */
    private data class Asked(val original: Long, val icons: Boolean, val text: Boolean, val controls: Boolean)
}
