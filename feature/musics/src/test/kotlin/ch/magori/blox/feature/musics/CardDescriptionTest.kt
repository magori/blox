package ch.magori.blox.feature.musics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** What TalkBack says of the info line of a card: words, without « · » nor « 0:24 ». */
internal class CardDescriptionTest {
    @Test
    fun `the info of a card is said as a sentence, its style, its key and its length in words`() {
        // When
        val said = cardInfoDescription(
            style = "Calme",
            key = "fa majeur",
            spokenLength = "24 secondes",
            listening = null,
        )

        // Then
        assertThat(said).isEqualTo("Calme, fa majeur, 24 secondes")
    }

    @Test
    fun `while the music is listened to, the bar played is said in place of its style, its key and its length`() {
        // When
        val said = cardInfoDescription(
            style = "Rétro",
            key = "sol mixolydien",
            spokenLength = "12 secondes",
            listening = Listening(bar = 3, bars = 8),
        )

        // Then
        assertThat(said).isEqualTo("Lecture, mesure 3 sur 8")
    }
}
