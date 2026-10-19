package ch.magori.blox.core.data

import app.cash.turbine.test
import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The contract of [MusicLibrary], what every implementation does, the real one and the fake shared by the tests of
 * the features alike. A subclass gives an empty library; what is proper to an implementation (a file, its errors) is
 * tested in the subclass.
 */
abstract class MusicLibraryContract {
    /** A library without any saved music, whatever it needs to live in [scope] (a DataStore does). */
    protected abstract fun emptyLibrary(scope: CoroutineScope): MusicLibrary

    @Test
    fun `without a saved music the list is empty and no music is played during the games`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)

        // When / Then
        assertThat(library.musics.first()).isEmpty()
        assertThat(library.gameMusic.first()).isNull()
    }

    @Test
    fun `a new music saved goes after the others`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))

        // When
        library.save(aMusic("b", name = "Joyeux"))

        // Then
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Calme"), aMusic("b", name = "Joyeux"))
    }

    @Test
    fun `a music saved again replaces the saved one at its place`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))

        // When the first one is changed: its tempo, its name
        library.save(aMusic("a", name = "Calme et lent", bpm = 80))

        // Then
        assertThat(library.musics.first()).containsExactly(
            aMusic("a", name = "Calme et lent", bpm = 80),
            aMusic("b", name = "Joyeux"),
        )
    }

    @Test
    fun `a music whose name another one has, the case ignored, is refused and nothing is saved`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Joyeux"))

        // When
        val failure = runCatching { library.save(aMusic("b", name = "JOYEUX")) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Joyeux"))
    }

    @Test
    fun `a music saved again may keep its name, in another case`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Joyeux"))

        // When
        library.save(aMusic("a", name = "JOYEUX"))

        // Then
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "JOYEUX"))
    }

    @Test
    fun `a renamed music keeps its place and its score`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))

        // When
        library.rename(ScoreId.Personal("a"), "Berceuse")

        // Then
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Berceuse"), aMusic("b", name = "Joyeux"))
    }

    @Test
    fun `a music cannot be renamed with the name of another one, the case ignored`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))

        // When
        val failure = runCatching { library.rename(ScoreId.Personal("a"), "joyeux") }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.musics.first().map { it.name }).containsExactly("Calme", "Joyeux")
    }

    @Test
    fun `a music may be renamed with its own name in another case`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))

        // When
        library.rename(ScoreId.Personal("a"), "CALME")

        // Then
        assertThat(library.musics.first().map { it.name }).containsExactly("CALME")
    }

    @Test
    fun `renaming a music that is not saved is refused`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))

        // When
        val failure = runCatching { library.rename(ScoreId.Personal("b"), "Joyeux") }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Calme"))
    }

    @Test
    fun `the music chosen for the games is played during the games, as it is saved now`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))

        // When it is chosen, then renamed
        library.setGameMusic(ScoreId.Personal("b"))
        library.rename(ScoreId.Personal("b"), "Gai")

        // Then
        assertThat(library.gameMusic.first()).isEqualTo(aMusic("b", name = "Gai"))
    }

    @Test
    fun `no music chosen for the games plays the music of the home screen again`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.setGameMusic(ScoreId.Personal("a"))

        // When
        library.setGameMusic(null)

        // Then
        assertThat(library.gameMusic.first()).isNull()
    }

    @Test
    fun `a music that is not saved cannot be chosen for the games`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.setGameMusic(ScoreId.Personal("a"))

        // When
        val failure = runCatching { library.setGameMusic(ScoreId.Personal("b")) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.gameMusic.first()).isEqualTo(aMusic("a", name = "Calme"))
    }

    @Test
    fun `a deleted music leaves the list, and what is deleted tells its place`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))
        library.save(aMusic("c", name = "Épique"))

        // When
        val deleted = library.delete(ScoreId.Personal("b"))

        // Then
        assertThat(deleted).isEqualTo(DeletedMusic(aMusic("b", name = "Joyeux"), place = 1, wasGameMusic = false))
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Calme"), aMusic("c", name = "Épique"))
    }

    @Test
    fun `the music played during the games, once deleted, is no longer played, and what is deleted tells it was`() =
        runTest {
            // Given
            val library = emptyLibrary(backgroundScope)
            library.save(aMusic("a", name = "Calme"))
            library.setGameMusic(ScoreId.Personal("a"))

            // When
            val deleted = library.delete(ScoreId.Personal("a"))

            // Then
            assertThat(deleted?.wasGameMusic).isTrue()
            assertThat(library.gameMusic.first()).isNull()
            // A music saved again with the same identity is not played during the games
            library.save(aMusic("a", name = "Calme"))
            assertThat(library.gameMusic.first()).isNull()
        }

    @Test
    fun `deleting another music leaves the one played during the games`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))
        library.setGameMusic(ScoreId.Personal("a"))

        // When
        library.delete(ScoreId.Personal("b"))

        // Then
        assertThat(library.gameMusic.first()).isEqualTo(aMusic("a", name = "Calme"))
    }

    @Test
    fun `deleting a music that is not saved gives nothing back and changes nothing`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))

        // When
        val deleted = library.delete(ScoreId.Personal("b"))

        // Then
        assertThat(deleted).isNull()
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Calme"))
    }

    @Test
    fun `a deleted music restored comes back at its place`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))
        library.save(aMusic("c", name = "Épique"))
        val deleted = checkNotNull(library.delete(ScoreId.Personal("b")))

        // When
        library.restore(deleted)

        // Then
        assertThat(library.musics.first()).containsExactly(
            aMusic("a", name = "Calme"),
            aMusic("b", name = "Joyeux"),
            aMusic("c", name = "Épique"),
        )
        assertThat(library.gameMusic.first()).isNull()
    }

    @Test
    fun `the music played during the games, restored, is played during the games again`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.setGameMusic(ScoreId.Personal("a"))
        val deleted = checkNotNull(library.delete(ScoreId.Personal("a")))

        // When
        library.restore(deleted)

        // Then
        assertThat(library.gameMusic.first()).isEqualTo(aMusic("a", name = "Calme"))
    }

    @Test
    fun `a music restored after the musics before it were deleted comes back at the end of the list`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))
        val deleted = checkNotNull(library.delete(ScoreId.Personal("b")))
        library.delete(ScoreId.Personal("a"))

        // When
        library.restore(deleted)

        // Then
        assertThat(library.musics.first()).containsExactly(aMusic("b", name = "Joyeux"))
    }

    @Test
    fun `a music restored twice is in the list once`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        val deleted = checkNotNull(library.delete(ScoreId.Personal("a")))
        library.restore(deleted)

        // When
        library.restore(deleted)

        // Then
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Calme"))
    }

    @Test
    fun `a music whose name was taken since its deletion cannot be restored`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        val deleted = checkNotNull(library.delete(ScoreId.Personal("a")))
        library.save(aMusic("b", name = "calme"))

        // When
        val failure = runCatching { library.restore(deleted) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.musics.first()).containsExactly(aMusic("b", name = "calme"))
    }

    @Test
    fun `each new identity is another one`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)

        // When
        val ids = List(3) { library.newId() }

        // Then
        assertThat(ids).doesNotHaveDuplicates()
    }

    @Test
    fun `saving another music emits nothing on the music played during the games`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.setGameMusic(ScoreId.Personal("a"))

        library.gameMusic.test {
            assertThat(awaitItem()).isEqualTo(aMusic("a", name = "Calme"))

            // When
            library.save(aMusic("b", name = "Joyeux"))
            testScheduler.advanceUntilIdle()

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `the list emits each change once`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)

        library.musics.test {
            assertThat(awaitItem()).isEmpty()

            // When a music is saved, then chosen for the games
            library.save(aMusic("a", name = "Calme"))
            assertThat(awaitItem()).containsExactly(aMusic("a", name = "Calme"))
            library.setGameMusic(ScoreId.Personal("a"))
            testScheduler.advanceUntilIdle()

            // Then the list did not change: nothing is emitted
            expectNoEvents()
        }
    }

    @Test
    fun `restoring a music that was not played during the games leaves the one that is`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("a", name = "Calme"))
        library.save(aMusic("b", name = "Joyeux"))
        library.setGameMusic(ScoreId.Personal("a"))
        val deleted = checkNotNull(library.delete(ScoreId.Personal("b")))

        // When
        library.restore(deleted)

        // Then
        assertThat(library.gameMusic.first()).isEqualTo(aMusic("a", name = "Calme"))
    }

    @Test
    fun `a new identity is none of the saved musics`() = runTest {
        // Given musics saved under the identities a library might give, those of the fake
        val library = emptyLibrary(backgroundScope)
        library.save(aMusic("music-1", name = "Calme"))
        library.save(aMusic("music-2", name = "Joyeux"))

        // When
        val id = library.newId()

        // Then
        assertThat(id).isNotIn(ScoreId.Personal("music-1"), ScoreId.Personal("music-2"))
    }
}
