package ch.magori.blox.core.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GardenNamesTest {
    private val gardens = listOf(aKeptGarden("a", "Jardin calme"), aKeptGarden("b", "Orage"))

    @Test
    fun `a name another garden has, the case and the spaces around it ignored, is taken, but not the garden's own`() {
        // When / Then
        assertThat(gardens.nameProblem(" ORAGE ")).isEqualTo(NameProblem.TAKEN)
        assertThat(gardens.nameProblem("Orage", apartFrom = GardenId("b"))).isNull()
        assertThat(gardens.nameProblem("  ")).isEqualTo(NameProblem.BLANK)
        assertThat(gardens.nameProblem("x".repeat(41))).isEqualTo(NameProblem.TOO_LONG)
        assertThat(gardens.nameProblem("Pluie douce")).isNull()
    }

    @Test
    fun `a free name is the one wanted when no other garden has it, else numbered from 2`() {
        // When / Then
        assertThat(gardens.freeName(" Pluie douce ")).isEqualTo("Pluie douce")
        assertThat(gardens.freeName("orage")).isEqualTo("orage 2")
        assertThat(gardens.freeName("Orage", apartFrom = GardenId("b"))).isEqualTo("Orage")
    }

    @Test
    fun `the name proposed is « Jardin » then the name of the mood in lower case, numbered if it is taken`() {
        // Given the name of a mood as the screen shows it
        val none = emptyList<KeptGarden>()

        // When / Then
        assertThat(none.proposedName(moodName = "Mystérieux")).isEqualTo("Jardin mystérieux")
        assertThat(gardens.proposedName(moodName = "Calme")).isEqualTo("Jardin calme 2")
    }

    /** A garden kept under [key] and [name]. */
    private fun aKeptGarden(
        key: String,
        name: String,
    ) = KeptGarden(GardenId(key), name, A_GARDEN, MODIFIED_AT)
}
