package ch.magori.blox.core.locksmith

import ch.magori.blox.core.music.RomanNumeral
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

/**
 * `compose-stability.conf` declares the game of the Locksmith stable to Compose, which its screen receives: a mutable
 * field would make Compose skip a due recomposition. Each instance field of those classes, the cases of the sealed
 * ones included, must be final.
 */
class LocksmithImmutabilityTest {
    @Test
    fun `every instance field of the classes the screen receives is final`() {
        // Given the classes declared stable, and the cases of the sealed ones, each one met whatever is added
        val declared = listOf(Padlock::class.java, PadlockGame::class.java, RomanNumeral::class.java)
        val sealed = listOf(PadlockEvent::class.java, Hint::class.java)
        val classes = declared + sealed + sealed.flatMap { it.permittedSubclasses.orEmpty().asList() }

        // When
        val mutableFields = classes
            .flatMap { type -> type.declaredFields.asList() }
            .filterNot { field -> Modifier.isStatic(field.modifiers) || Modifier.isFinal(field.modifiers) }
            .map { field -> "${field.declaringClass.name}.${field.name}" }

        // Then
        assertThat(classes).contains(PadlockEvent.PadlockOpened::class.java, Hint.Blocking::class.java)
        assertThat(mutableFields).isEmpty()
    }
}
