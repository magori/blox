package ch.magori.blox.core.data

import ch.magori.blox.core.music.ScoreId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/** The rules of the names of the saved musics, shared by the repository, which keeps them, and the editor (P4.19). */
class MusicNamesTest {
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["Joyeux", "joyeux", "JOYEUX"])
    fun `a name is taken by a music of the same name, the case ignored`(name: String) {
        // Given
        val musics = listOf(aMusic("a", name = "Calme"), aMusic("b", name = "Joyeux"))

        // When / Then
        assertThat(musics.isNameTaken(name)).isTrue()
    }

    @Test
    fun `a name no music has is free`() {
        // Given
        val musics = listOf(aMusic("a", name = "Calme"))

        // When / Then
        assertThat(musics.isNameTaken("Joyeux")).isFalse()
    }

    @Test
    fun `the music being saved does not take its own name, in another case or not`() {
        // Given
        val musics = listOf(aMusic("a", name = "Joyeux"))

        // When / Then
        assertThat(musics.isNameTaken("JOYEUX", apartFrom = ScoreId.Personal("a"))).isFalse()
        assertThat(musics.isNameTaken("Joyeux", apartFrom = ScoreId.Personal("b"))).isTrue()
    }

    @Test
    fun `the spaces around a name do not make it another one`() {
        // Given
        val musics = listOf(aMusic("a", name = "Joyeux"))

        // When / Then
        assertThat(musics.isNameTaken(" joyeux  ")).isTrue()
    }

    @Test
    fun `a free name is the name wanted when no music has it`() {
        // Given
        val musics = listOf(aMusic("a", name = "Calme"))

        // When / Then
        assertThat(musics.freeName("Joyeux")).isEqualTo("Joyeux")
    }

    @Test
    fun `a taken name is followed by the first number from 2 that makes it free`() {
        // Given
        val musics = listOf(aMusic("a", name = "Joyeux"), aMusic("b", name = "joyeux 2"))

        // When / Then
        assertThat(musics.freeName("Joyeux")).isEqualTo("Joyeux 3")
        assertThat(musics.take(1).freeName("Joyeux")).isEqualTo("Joyeux 2")
    }

    @Test
    fun `the music being saved keeps its own name as a free one`() {
        // Given
        val musics = listOf(aMusic("a", name = "Joyeux"))

        // When / Then
        assertThat(musics.freeName("Joyeux", apartFrom = ScoreId.Personal("a"))).isEqualTo("Joyeux")
    }

    @Test
    fun `a taken name that ends with a number counts on from it`() {
        // Given
        val musics = listOf(aMusic("a", name = "Joyeux 2"))

        // When / Then
        assertThat(musics.freeName("Joyeux 2")).isEqualTo("Joyeux 3")
    }

    @Test
    fun `a free name leaves out the spaces around the name wanted`() {
        // Given
        val musics = listOf(aMusic("a", name = "Calme"))

        // When / Then
        assertThat(musics.freeName("  Joyeux ")).isEqualTo("Joyeux")
        assertThat(musics.freeName(" Calme ")).isEqualTo("Calme 2")
    }

    @Test
    fun `a taken name ending with a number too big to count on is followed by 2`() {
        // Given
        val musics = listOf(aMusic("a", name = "Joyeux 99999999999"))

        // When / Then
        assertThat(musics.freeName("Joyeux 99999999999")).isEqualTo("Joyeux 99999999999 2")
    }

    @Test
    fun `a name is taken whatever its case, its accents in capitals and the spaces around it`() {
        // Given
        val musics = listOf(aMusic("a", name = "Thème du soir"), aMusic("b", name = "Matin calme"))

        // When / Then
        assertThat(musics.isNameTaken("  THÈME DU SOIR ")).isTrue()
        assertThat(musics.isNameTaken("Thème du matin")).isFalse()
    }

    @Test
    fun `a taken name is offered free with the next number that no music has, the case of the name wanted kept`() {
        // Given
        val musics = listOf(
            aMusic("a", name = "Thème"),
            aMusic("b", name = "Thème 2"),
            aMusic("c", name = "Thème 3"),
            aMusic("d", name = "Joyeux 1"),
        )

        // When / Then
        assertThat(musics.freeName("thème ")).isEqualTo("thème 4")
        assertThat(musics.freeName("Thème 2")).isEqualTo("Thème 4")
        assertThat(musics.freeName("joyeux 1")).isEqualTo("joyeux 2")
    }

    @ParameterizedTest(name = "« {0} »: {1}")
    @CsvSource(
        nullValues = ["none"],
        value = [
            "Thème du matin, none",
            "' Thème du matin ', none",
            "ma marche, TAKEN",
            "' Ma marche  ', TAKEN",
            "'', BLANK",
            "'   ', BLANK",
        ],
    )
    fun `a name is refused when blank, or taken by another music, the case and the spaces ignored`(
        name: String,
        problem: NameProblem?,
    ) {
        // Given
        val musics = listOf(aMusic("a", name = "Thème du soir"), aMusic("b", name = "Ma marche"))

        // When / Then
        assertThat(musics.nameProblem(name)).isEqualTo(problem)
    }

    @Test
    fun `a name of more characters than a saved name may have is refused, the spaces around it apart`() {
        // Given
        val longest = "x".repeat(SavedMusic.MAX_NAME_LENGTH)

        // When / Then
        assertThat(emptyList<SavedMusic>().nameProblem(" $longest ")).isNull()
        assertThat(emptyList<SavedMusic>().nameProblem("x".repeat(SavedMusic.MAX_NAME_LENGTH + 1)))
            .isEqualTo(NameProblem.TOO_LONG)
    }

    @Test
    fun `the music being renamed may keep its own name, in another case or not`() {
        // Given
        val musics = listOf(aMusic("a", name = "Joyeux"))

        // When / Then
        assertThat(musics.nameProblem("JOYEUX", apartFrom = ScoreId.Personal("a"))).isNull()
        assertThat(musics.nameProblem("Joyeux", apartFrom = ScoreId.Personal("b"))).isEqualTo(NameProblem.TAKEN)
    }

    @Test
    fun `a name of the most characters a saved name may have is accepted, its characters counted and not its units`() {
        // Given a name of 40 characters, each written with two units
        val longest = NOTE.repeat(SavedMusic.MAX_NAME_LENGTH)

        // When / Then
        assertThat(emptyList<SavedMusic>().nameProblem(longest)).isNull()
        assertThat(emptyList<SavedMusic>().nameProblem("$longest!")).isEqualTo(NameProblem.TOO_LONG)
    }

    @Test
    fun `a name typed is cut at the most characters a saved name may have, a shorter one kept as it is`() {
        // Given
        val longest = "x".repeat(SavedMusic.MAX_NAME_LENGTH)

        // When / Then
        assertThat(cutName(longest + "yz")).isEqualTo(longest)
        assertThat(cutName(longest)).isEqualTo(longest)
        assertThat(cutName(" Joyeux ")).isEqualTo(" Joyeux ")
    }

    @Test
    fun `a name typed is cut after its characters, never between the two units of one`() {
        // Given a name of 41 characters, each written with two units
        val pasted = NOTE.repeat(SavedMusic.MAX_NAME_LENGTH + 1)

        // When / Then
        assertThat(cutName(pasted)).isEqualTo(NOTE.repeat(SavedMusic.MAX_NAME_LENGTH))
    }

    private companion object {
        /** A musical note, a character written with two units of UTF-16. */
        const val NOTE = "\uD83C\uDFB5"
    }
}
