package ch.magori.blox.core.data

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KeptGardenTest {
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", "   "])
    fun `a kept garden has a name that is not blank`(name: String) {
        assertThatIllegalArgumentException().isThrownBy { aKeptGarden(name) }
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = [" Calme", "Calme ", "\tCalme"])
    fun `a kept garden has no spaces around its name`(name: String) {
        assertThatIllegalArgumentException().isThrownBy { aKeptGarden(name) }
    }

    @Test
    fun `a kept garden has a name of 40 characters at the most, a character of two units counting for one`() {
        // Given 40 moons, each one character of two units of UTF-16
        val forty = "🌙".repeat(40)

        // When / Then
        assertThat(aKeptGarden(forty).name).isEqualTo(forty)
        assertThatIllegalArgumentException().isThrownBy { aKeptGarden("x".repeat(41)) }
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", "   "])
    fun `a kept garden is known by a key that is not blank`(key: String) {
        assertThatIllegalArgumentException().isThrownBy { GardenId(key) }
    }

    /** A garden kept under [name]. */
    private fun aKeptGarden(name: String) = KeptGarden(GardenId("a"), name, A_GARDEN, MODIFIED_AT)
}
