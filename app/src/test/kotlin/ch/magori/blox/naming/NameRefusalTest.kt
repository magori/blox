package ch.magori.blox.naming

import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.designsystem.text.NameRefusal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Mes musiques, the editor and the garden tell a name refused by `NameRefusal.valueOf(problem.name)`: `core:designsystem`
 * does not see the `NameProblem` of `core:data`. Here in app, which knows the three features.
 */
internal class NameRefusalTest {
    @Test
    fun `each problem of a name has the refusal of the same name, and each refusal its problem`() {
        // When
        val refusals = NameProblem.entries.map { NameRefusal.valueOf(it.name) }

        // Then
        assertThat(refusals).containsExactlyInAnyOrderElementsOf(NameRefusal.entries)
    }
}
