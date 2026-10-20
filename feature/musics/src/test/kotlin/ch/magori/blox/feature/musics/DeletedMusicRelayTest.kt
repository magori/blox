package ch.magori.blox.feature.musics

import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DeletedMusicRelayTest {
    private val relay = DeletedMusicRelay()

    @Test
    fun `a music handed over is taken once, then no more`() {
        // Given
        val evening = aDeletedMusic("a", "Thème du soir")
        relay.handOver(evening)

        // When / Then
        assertThat(relay.take(evening)).isTrue()
        assertThat(relay.take(evening)).isFalse()
    }

    @Test
    fun `a music handed over in place of one not taken replaces it`() {
        // Given
        val evening = aDeletedMusic("a", "Thème du soir")
        val march = aDeletedMusic("b", "Ma marche")
        relay.handOver(evening)

        // When
        relay.handOver(march)

        // Then
        assertThat(relay.take(evening)).isFalse()
        assertThat(relay.take(march)).isTrue()
    }

    @Test
    fun `of two lists waiting for a music handed over, only one takes it`() = runTest {
        // Given two lists waiting, as the view model of the list waits
        val evening = aDeletedMusic("a", "Thème du soir")
        val taken = mutableListOf<DeletedMusic>()
        repeat(2) {
            backgroundScope.launch {
                relay.handedOver.collect { handed -> if (relay.take(handed)) taken += handed }
            }
        }
        runCurrent()

        // When
        relay.handOver(evening)
        runCurrent()

        // Then
        assertThat(taken).containsExactly(evening)
    }

    /** A music of one bar called [name], of the key [key], deleted from the first place of the list. */
    private fun aDeletedMusic(
        key: String,
        name: String,
    ): DeletedMusic {
        val id = ScoreId.Personal(key)
        val voice = Voice(Waveform.SQUARE, 0.1, listOf(Note(Pitch.parse("C4"), 4.0)))
        val score = Score(id, bpm = 120, beatsPerBar = 4, voices = listOf(voice))
        val music = SavedMusic(id, name, category = null, Scale(Pitch.parse("C4"), ScaleType.MAJOR), score)
        return DeletedMusic(music, place = 0, wasGameMusic = false)
    }
}
