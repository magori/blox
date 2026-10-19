package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * `compose-stability.conf` declares every class of this package stable to Compose: a mutable field would make
 * Compose skip a due recomposition. Each instance field of the package, nested classes included, must be final.
 */
class ModelImmutabilityTest {
    @Test
    fun `every instance field of the model package is final`() {
        val classes = modelClasses()

        val mutableFields = classes
            .flatMap { type -> type.declaredFields.asList() }
            .filterNot { field -> Modifier.isStatic(field.modifiers) || Modifier.isFinal(field.modifiers) }
            .map { field -> "${field.declaringClass.name}.${field.name}" }

        assertThat(classes).contains(Board::class.java, Piece::class.java, Cell.Filled::class.java)
        assertThat(mutableFields).isEmpty()
    }

    /** The classes of the package, read where [Board] was loaded from: a directory or a jar. */
    private fun modelClasses(): List<Class<*>> {
        val packagePath = Board::class.java.packageName.replace('.', '/')
        val location = File(Board::class.java.protectionDomain.codeSource.location.toURI())
        val entries = if (location.isDirectory) {
            location.resolve(packagePath).list().orEmpty().map { name -> "$packagePath/$name" }
        } else {
            JarFile(location).use { jar -> jar.entries().asSequence().map(JarEntry::getName).toList() }
        }
        return entries
            .filter { entry -> entry.endsWith(".class") && entry.substringBeforeLast('/') == packagePath }
            .map { entry -> Class.forName(entry.removeSuffix(".class").replace('/', '.')) }
    }
}
