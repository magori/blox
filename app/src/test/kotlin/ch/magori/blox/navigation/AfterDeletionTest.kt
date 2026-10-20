package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
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
import ch.magori.blox.feature.composer.navigation.ComposerRoute
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.musics.navigation.MyMusicsRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AfterDeletionTest {
    /** The musics handed over to « Mes musiques », as the relay of `app` receives them. */
    private val handedOver = mutableListOf<DeletedMusic>()

    private val deleted = aDeletedMusic()

    @Test
    fun `a deletion in the editor opened from my musics goes back to my musics, and hands the music over to it`() {
        // Given the editor of a saved music, opened from the list
        val backStack = mutableListOf<NavKey>(HomeRoute, MyMusicsRoute, ComposerRoute(musicKey = "a"))

        // When
        afterDeletion(backStack, deleted, handedOver::add)

        // Then
        assertThat(backStack).containsExactly(HomeRoute, MyMusicsRoute)
        assertThat(handedOver).containsExactly(deleted)
    }

    @Test
    fun `a deletion in the editor opened from home opens my musics in its place, over home, handing the music over`() {
        // Given the editor of a new music, opened from home, then saved
        val backStack = mutableListOf<NavKey>(HomeRoute, ComposerRoute())

        // When
        afterDeletion(backStack, deleted, handedOver::add)

        // Then
        assertThat(backStack).containsExactly(HomeRoute, MyMusicsRoute)
        assertThat(handedOver).containsExactly(deleted)
    }

    @Test
    fun `once the editor has left, a late deletion changes nothing and hands nothing over`() {
        // Given the editor already closed
        val backStack = mutableListOf<NavKey>(HomeRoute, MyMusicsRoute)

        // When
        afterDeletion(backStack, deleted, handedOver::add)

        // Then
        assertThat(backStack).containsExactly(HomeRoute, MyMusicsRoute)
        assertThat(handedOver).isEmpty()
    }

    @Test
    fun `the editor alone on the stack leaves my musics alone in its place`() {
        // Given a stack the application never builds: the editor, without home under it
        val backStack = mutableListOf<NavKey>(ComposerRoute())

        // When
        afterDeletion(backStack, deleted, handedOver::add)

        // Then
        assertThat(backStack).containsExactly(MyMusicsRoute)
        assertThat(handedOver).containsExactly(deleted)
    }

    @Test
    fun `with my musics lower in the stack, the screens above it leave with the editor, never a second list`() {
        // Given a stack the application never builds: another screen between the list and the editor
        val game = GameRoute(startLevel = 0, seed = 1L)
        val backStack = mutableListOf<NavKey>(HomeRoute, MyMusicsRoute, game, ComposerRoute())

        // When
        afterDeletion(backStack, deleted, handedOver::add)

        // Then
        assertThat(backStack).containsExactly(HomeRoute, MyMusicsRoute)
        assertThat(handedOver).containsExactly(deleted)
    }

    /** A music of one bar, deleted from the second place of the list. */
    private fun aDeletedMusic(): DeletedMusic {
        val id = ScoreId.Personal("a")
        val voice = Voice(Waveform.SQUARE, 0.1, listOf(Note(Pitch.parse("C4"), 4.0)))
        val score = Score(id, bpm = 120, beatsPerBar = 4, voices = listOf(voice))
        val music = SavedMusic(id, "Thème du soir", category = null, Scale(Pitch.parse("C4"), ScaleType.MAJOR), score)
        return DeletedMusic(music, place = 1, wasGameMusic = false)
    }
}
