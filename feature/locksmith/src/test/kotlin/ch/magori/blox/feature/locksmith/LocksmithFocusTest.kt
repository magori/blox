package ch.magori.blox.feature.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Where the focus goes when the padlock opens or starts over: told once, by the card, then by the message. */
class LocksmithFocusTest {
    @Test
    fun `the title of the card takes the focus when the padlock opens`() {
        assertThat(focusAfter(wasOpen = false, isOpen = true)).isEqualTo(FocusTarget.CARD_TITLE)
    }

    @Test
    fun `replaying an open padlock gives the focus to the message`() {
        assertThat(focusAfter(wasOpen = true, isOpen = false)).isEqualTo(FocusTarget.MESSAGE)
    }

    @Test
    fun `nothing takes the focus while the padlock stays closed or stays open, as on coming back to the screen`() {
        assertThat(focusAfter(wasOpen = false, isOpen = false)).isNull()
        assertThat(focusAfter(wasOpen = true, isOpen = true)).isNull()
    }
}
