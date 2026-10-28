package ch.magori.blox.core.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * The rules of the names of a list, whatever it names (P10.25): shown here on gardens known by an identity of their
 * own, as « Mes jardins » will keep them; the saved musics follow the same rules ([MusicNamesTest]).
 */
class TakenNamesTest {
    @Test
    fun `a name is taken by another of the list, whatever its case and the spaces around it`() {
        // Given
        val gardens = TakenNames(mapOf("g1" to "Jardin d'hiver", "g2" to "Pluie"))

        // When / Then
        assertThat(gardens.isTaken("  JARDIN D'HIVER ")).isTrue()
        assertThat(gardens.isTaken("Jardin d'été")).isFalse()
    }

    @Test
    fun `a name is refused when blank, too long or taken by another, the one renamed keeping its own`() {
        // Given
        val gardens = TakenNames(mapOf("g1" to "Jardin d'hiver"))

        // When / Then
        assertThat(gardens.problemOf(" Pluie ")).isNull()
        assertThat(gardens.problemOf("  ")).isEqualTo(NameProblem.BLANK)
        assertThat(gardens.problemOf("x".repeat(TakenNames.MAX_LENGTH + 1))).isEqualTo(NameProblem.TOO_LONG)
        assertThat(gardens.problemOf("jardin d'hiver")).isEqualTo(NameProblem.TAKEN)
        assertThat(gardens.problemOf("jardin d'hiver", apartFrom = "g1")).isNull()
    }

    @Test
    fun `the one renamed does not take its own name, another one does`() {
        // Given
        val gardens = TakenNames(mapOf("g1" to "Pluie"))

        // When / Then
        assertThat(gardens.isTaken(" PLUIE ", apartFrom = "g1")).isFalse()
        assertThat(gardens.isTaken("Pluie", apartFrom = "g2")).isTrue()
    }

    @Test
    fun `a name is weighed by its characters and not its units, two units for each of them`() {
        // Given
        val gardens = TakenNames(emptyMap<String, String>())
        val longest = NOTE.repeat(TakenNames.MAX_LENGTH)

        // When / Then
        assertThat(gardens.problemOf(longest)).isNull()
        assertThat(gardens.problemOf(longest + NOTE)).isEqualTo(NameProblem.TOO_LONG)
    }

    @Test
    fun `a free name is the one wanted, else followed by the first number that makes it free, from its own`() {
        // Given
        val gardens = TakenNames(mapOf("g1" to "Pluie", "g2" to "pluie 2", "g3" to "Rosée 4"))

        // When / Then
        assertThat(gardens.freeName(" Brume ")).isEqualTo("Brume")
        assertThat(gardens.freeName("Pluie ")).isEqualTo("Pluie 3")
        assertThat(gardens.freeName("Rosée 4")).isEqualTo("Rosée 5")
        assertThat(gardens.freeName("Pluie", apartFrom = "g1")).isEqualTo("Pluie")
    }

    @Test
    fun `a name proposed is one of those no other has, else one numbered, else one cut so that its number fits`() {
        // Given
        val longest = "x".repeat(TakenNames.MAX_LENGTH)
        val gardens = TakenNames(mapOf("g1" to "Pluie", "g2" to longest))

        // When / Then
        assertThat(gardens.proposedAmong(listOf("Pluie", "Brume"), Random(SEED))).isEqualTo("Brume")
        assertThat(gardens.proposedAmong(listOf("Pluie"), Random(SEED))).isEqualTo("Pluie 2")
        assertThat(gardens.proposedAmong(listOf(longest), Random(SEED))).isEqualTo(longest.dropLast(2) + " 2")
    }

    private companion object {
        const val SEED = 7

        /** A musical note, a character written with two units of UTF-16. */
        const val NOTE = "🎵"
    }
}
