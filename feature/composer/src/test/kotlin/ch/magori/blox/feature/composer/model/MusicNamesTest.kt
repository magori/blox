package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MusicNamesTest {
    @Test
    fun `the proposed name numbers the category, the first number that no music has`() {
        // Given
        val names = MusicNames(listOf("Calme 1", "joyeux 1", "Joyeux 3"))

        // Then
        assertThat(names.proposed("Joyeux")).isEqualTo("Joyeux 2")
        assertThat(names.proposed("Épique")).isEqualTo("Épique 1")
    }

    @Test
    fun `a name is taken by another music whatever its case and the spaces around it`() {
        // Given
        val names = MusicNames(listOf("Thème du soir", "Matin calme"))

        // Then
        assertThat(names.isTaken("  THÈME DU SOIR ")).isTrue()
        assertThat(names.isTaken("Thème du matin")).isFalse()
    }

    @Test
    fun `the name of the music being edited is not taken by it`() {
        // Given « Matin calme » is being edited
        val names = MusicNames(listOf("Thème du soir", "Matin calme"), current = "Matin calme")

        // Then
        assertThat(names.isTaken("matin calme")).isFalse()
        assertThat(names.isTaken("Thème du soir")).isTrue()
    }

    @Test
    fun `a taken name is offered free with the first number from 2 that no music has`() {
        // Given
        val names = MusicNames(listOf("Thème", "Thème 2", "Matin"))

        // Then
        assertThat(names.free("thème ")).isEqualTo("thème 3")
        assertThat(names.free("Matin")).isEqualTo("Matin 2")
    }

    @Test
    fun `a taken name that ends with a number is offered free with the next number not taken`() {
        // Given
        val names = MusicNames(listOf("Thème 2", "Thème 3", "Joyeux 1"))

        // Then
        assertThat(names.free("Thème 2")).isEqualTo("Thème 4")
        assertThat(names.free("joyeux 1")).isEqualTo("joyeux 2")
    }
}
