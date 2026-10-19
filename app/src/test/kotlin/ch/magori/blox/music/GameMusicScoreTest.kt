package ch.magori.blox.music

import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameMusicScoreTest {
    private val library = FakeMusicLibrary()

    @Test
    fun `without a music chosen for the games no score is read`() = runTest {
        // Given a music saved, not chosen for the games
        library.save(aMusic("Calme 1"))

        // When / Then
        assertThat(library.gameMusicScore()).isNull()
    }

    @Test
    fun `the score read is the one of the music chosen for the games`() = runTest {
        // Given two musics saved, the first one chosen for the games
        val chosen = aMusic("Calme 1")
        library.save(chosen)
        library.save(aMusic("Calme 2"))
        library.setGameMusic(chosen.id)

        // When / Then
        assertThat(library.gameMusicScore()).isEqualTo(chosen.score)
    }

    /** A music of the player called [name]: the minuet, under a new personal identity. */
    private fun aMusic(name: String): SavedMusic {
        val id = library.newId()
        return SavedMusic(id, name, null, Scale(Pitch.parse("C4"), ScaleType.MAJOR), Scores.minuet.copy(id = id))
    }
}
