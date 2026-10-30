package ch.magori.blox.core.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PendingDeletionTest {
    private val pending = PendingDeletion<String>()

    @Test
    fun `the element after the one deleted takes the focus`() {
        // Given
        pending.waitFor("b", listOf("a", "b", "c"))

        // When / Then
        assertThat(pending.left(listOf("a", "c"))).isEqualTo(FocusTarget.Element("c"))
    }

    @Test
    fun `the last element deleted gives the focus to the one before it`() {
        // Given
        pending.waitFor("c", listOf("a", "b", "c"))

        // When / Then
        assertThat(pending.left(listOf("a", "b"))).isEqualTo(FocusTarget.Element("b"))
    }

    @Test
    fun `the only element deleted gives the focus to the empty list`() {
        // Given
        pending.waitFor("a", listOf("a"))

        // When / Then
        assertThat(pending.left(emptyList())).isEqualTo(FocusTarget.EmptyList)
    }

    @Test
    fun `while the element awaited is still listed, nothing takes the focus, and it is still awaited`() {
        // Given
        pending.waitFor("b", listOf("a", "b", "c"))

        // When / Then
        assertThat(pending.left(listOf("a", "b", "c"))).isNull()
        assertThat(pending.left(listOf("a", "c"))).isEqualTo(FocusTarget.Element("c"))
    }

    @Test
    fun `an element gone gives the focus once only, once it is given`() {
        // Given
        pending.waitFor("b", listOf("a", "b", "c"))
        pending.left(listOf("a", "c"))
        pending.given()

        // When / Then
        assertThat(pending.left(listOf("a"))).isNull()
    }

    @Test
    fun `an element gone still gives the focus while it has not been given`() {
        // Given the focus found for « c », then the effect that would give it cancelled before it does
        pending.waitFor("b", listOf("a", "b", "c"))
        pending.left(listOf("a", "c"))

        // When / Then
        assertThat(pending.left(listOf("a", "c"))).isEqualTo(FocusTarget.Element("c"))
    }

    @Test
    fun `the window of the same element opened again after a deletion not confirmed takes the order anew`() {
        // Given the deletion of « b » not confirmed, then its window opened again, « d » listed since
        pending.waitFor("b", listOf("a", "b", "c"))
        pending.waitFor("b", listOf("a", "b", "d", "c"))

        // When / Then
        assertThat(pending.left(listOf("a", "d", "c"))).isEqualTo(FocusTarget.Element("d"))
    }

    @Test
    fun `the element after the one deleted is the next one still listed, in the order as the window was open`() {
        // Given
        pending.waitFor("b", listOf("a", "b", "c", "d"))

        // When « c » left too / Then
        assertThat(pending.left(listOf("a", "d"))).isEqualTo(FocusTarget.Element("d"))
    }

    @Test
    fun `a list read without the element while its window is open keeps the order as the window opened`() {
        // Given the window of « b » open, then the list read without it, the window still open
        pending.waitFor("b", listOf("a", "b", "c"))
        pending.waitFor("b", listOf("a", "c"))

        // When / Then
        assertThat(pending.left(listOf("a", "c"))).isEqualTo(FocusTarget.Element("c"))
    }

    @Test
    fun `the only element read out of the list while its window is open gives the focus to the empty list`() {
        // Given the window of « a », the only element, open, then the list read empty, the window still open
        pending.waitFor("a", listOf("a"))
        pending.waitFor("a", emptyList())

        // When / Then
        assertThat(pending.left(emptyList())).isEqualTo(FocusTarget.EmptyList)
    }

    @Test
    fun `an element awaited that was never in the order gives the focus to the first element listed`() {
        // Given
        pending.waitFor("z", listOf("a", "b"))

        // When / Then
        assertThat(pending.left(listOf("a", "b"))).isEqualTo(FocusTarget.Element("a"))
    }

    @Test
    fun `nothing awaited gives the focus to nothing`() {
        // Given
        pending.waitFor(null, emptyList())

        // When / Then
        assertThat(pending.left(emptyList())).isNull()
    }
}
