package ch.magori.blox.core.garden

import ch.magori.blox.core.music.Accompaniment
import ch.magori.blox.core.music.LiveMusic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The musics composed by the games, all known by a fixed `ScoreId.Personal`. Tested in `core:garden`, the one module
 * that sees the four: the garden here, the live music and the accompaniment of Au tempo in `core:music`.
 */
class GameMusicKeysTest {
    @Test
    fun `the musics composed by the games each have their own key`() {
        // Given
        val ids = listOf(
            LiveMusic.CALM_ID,
            LiveMusic.TENSE_ID,
            GARDEN_SCORE_ID,
            Accompaniment.score(metronome = false).id,
        )

        // When / Then
        assertThat(ids).doesNotHaveDuplicates()
    }
}
