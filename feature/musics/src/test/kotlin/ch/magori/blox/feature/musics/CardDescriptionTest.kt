package ch.magori.blox.feature.musics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** What TalkBack says of the info lines of a card: words, without « · » nor « 0:24 ». */
internal class CardDescriptionTest {
    @Test
    fun `the info of a card is said as a sentence, its tempo per minute, its length and its day in words`() {
        // When
        val said = cardInfoDescription(
            style = "Calme",
            key = "fa majeur",
            bpm = 80,
            spokenLength = "24 secondes",
            spokenDay = "18 octobre",
            listening = null,
        )

        // Then
        assertThat(said).isEqualTo("Calme, fa majeur, 80 temps par minute, 24 secondes, modifiée le 18 octobre")
    }

    @Test
    fun `while the music is listened to, the bar played is said in place of its tempo and its length`() {
        // When
        val said = cardInfoDescription(
            style = "Rétro",
            key = "sol mixolydien",
            bpm = 156,
            spokenLength = "12 secondes",
            spokenDay = "1er octobre",
            listening = Listening(bar = 3, bars = 8),
        )

        // Then
        assertThat(said).isEqualTo("Rétro, sol mixolydien, lecture, mesure 3 sur 8, modifiée le 1er octobre")
    }

    @Test
    fun `a music whose day is unknown is said without it`() {
        // When
        val said = cardInfoDescription(
            style = "Libre",
            key = "la mineur",
            bpm = 132,
            spokenLength = "15 secondes",
            spokenDay = null,
            listening = null,
        )

        // Then
        assertThat(said).isEqualTo("Libre, la mineur, 132 temps par minute, 15 secondes")
    }
}
