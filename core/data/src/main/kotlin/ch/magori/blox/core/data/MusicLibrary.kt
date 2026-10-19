package ch.magori.blox.core.data

import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.flow.Flow

/**
 * The musics of the player, « Mes musiques » (P4.22), kept on the device, and the one played during the games, if any.
 *
 * Each change fails with an [java.io.IOException] if the musics cannot be written: the only failure a screen catches.
 * An [IllegalArgumentException] is a precondition the caller broke, a bug, not to be caught: the screen checks a
 * name with [isNameTaken] before it saves, renames or restores a music, and changes only a music of the list.
 */
interface MusicLibrary {
    /**
     * A new identity, for a music the editor opens before it is saved (a new one, a generated one): no saved music has
     * it, nor any other identity this library gave.
     */
    fun newId(): ScoreId.Personal

    /**
     * The saved musics, in the order they were first saved.
     *
     * Never fails with an [java.io.IOException]: musics that cannot be read give an empty list, once, and the flow then
     * ends. Any other failure is a bug, and goes through to the collector: it is not to be caught.
     */
    val musics: Flow<List<SavedMusic>>

    /**
     * The saved music played during the games, instead of the music chosen on the home screen; null when there is none,
     * and the games play the music of the home screen. Fails as [musics] does.
     */
    val gameMusic: Flow<SavedMusic?>

    /**
     * Saves [music]: a new one goes after the others, one saved before takes the place of its saved version.
     * A name another saved music has, the case and the spaces around it ignored ([isNameTaken]), is refused.
     *
     * @throws java.io.IOException if the musics cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if another music has the name of [music]: a precondition, see [isNameTaken].
     * Also if a voice of [music] holds more notes than the file may keep (65 536), far beyond what the editor writes.
     */
    suspend fun save(music: SavedMusic)

    /**
     * Renames the music of [id] [name]; it keeps its place and its score.
     *
     * @throws java.io.IOException if the musics cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if no music is known by [id], or if another one is called [name]: preconditions,
     * see [isNameTaken].
     */
    suspend fun rename(
        id: ScoreId.Personal,
        name: String,
    )

    /**
     * Plays the music of [id] during the games from now on, instead of the music chosen on the home screen; null plays
     * the music of the home screen again. One music at most is played during the games: choosing one leaves the other.
     *
     * @throws java.io.IOException if the musics cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if no music is known by [id]: a precondition.
     */
    suspend fun setGameMusic(id: ScoreId.Personal?)

    /**
     * Deletes the music of [id]; it is no longer played during the games if it was. What it gives back lets
     * [restore] put the music back; null when no music is known by [id], and nothing changes.
     *
     * @throws java.io.IOException if the musics cannot be written; any other failure is a bug.
     */
    suspend fun delete(id: ScoreId.Personal): DeletedMusic?

    /**
     * Puts a [deleted] music back at its place, « Annuler » after a deletion: at the end of the list at the most, played
     * during the games again if it was. A music back already stays as it is.
     *
     * @throws java.io.IOException if the musics cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if another music took its name since: a precondition, see [isNameTaken].
     */
    suspend fun restore(deleted: DeletedMusic)
}
