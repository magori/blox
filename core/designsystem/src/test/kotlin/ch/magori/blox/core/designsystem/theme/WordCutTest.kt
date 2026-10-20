package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WordCutTest {
    @Test
    fun `a text on one line cuts no word`() {
        assertThat(cutsAWord("Enregistrer", lineEnds = emptyList())).isFalse()
    }

    @Test
    fun `a text that goes to the line between its words cuts no word`() {
        // « Garder la » / « musique »
        assertThat(cutsAWord("Garder la musique", lineEnds = listOf(10))).isFalse()
    }

    @Test
    fun `a text that goes to the line inside a word cuts it`() {
        // « Annul » / « er »
        assertThat(cutsAWord("Annuler", lineEnds = listOf(5))).isTrue()
    }

    @Test
    fun `a word cut on any of the lines is enough`() {
        // « Enreg » / « istre » / « r »
        assertThat(cutsAWord("Enregistrer", lineEnds = listOf(5, 10))).isTrue()
    }

    @Test
    fun `a text that goes to the line after a slash or a hyphen cuts its word, a line goes on only after a space`() {
        // « 120 temps/ » / « min »
        assertThat(cutsAWord("120 temps/min", lineEnds = listOf(10))).isTrue()
        assertThat(cutsAWord("pas-à-pas", lineEnds = listOf(4))).isTrue()
    }

    @Test
    fun `a number cut between two digits is cut`() {
        assertThat(cutsAWord("12345", lineEnds = listOf(2))).isTrue()
    }
}
