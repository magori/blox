package ch.magori.blox.core.data

import ch.magori.blox.core.music.ScoreId
import java.time.Instant

/**
 * What a [MusicLibrary] holds: the saved [musics], in the order they were first saved, and the identity of the one
 * played during the games, if any, [gameMusicId]. Its changes are the rules of the library, shared by the real one
 * and the fake: each one gives a new content, or refuses with an [IllegalArgumentException] what would break them (two
 * musics of the same name, a music that is not saved). A change dates the music it changes, to the millisecond, as
 * the musics file keeps it: the music read before the file is opened again is the one read after.
 */
internal data class MusicLibraryContent(
    val musics: List<SavedMusic> = emptyList(),
    val gameMusicId: ScoreId.Personal? = null,
) {
    /** The saved music played during the games; null when there is none. */
    val gameMusic: SavedMusic? get() = musics.firstOrNull { it.id == gameMusicId }

    /** [music], changed [at] that time, in place of the one of its identity, or after them all if it is new. */
    fun saving(
        music: SavedMusic,
        at: Instant,
    ): MusicLibraryContent {
        requireFreeName(music.name, music.id)
        val place = musics.indexOfFirst { it.id == music.id }
        val dated = music.copy(modifiedAt = at.toFileTime())
        return copy(musics = if (place < 0) musics + dated else musics.replacing(place, dated))
    }

    /**
     * The music of [id] called [name], at its place, changed [at] that time; unchanged if it is called [name] already,
     * the very same: nothing of it changes.
     */
    fun renaming(
        id: ScoreId.Personal,
        name: String,
        at: Instant,
    ): MusicLibraryContent {
        val place = placeOf(id)
        requireFreeName(name, id)
        if (musics[place].name == name) return this
        return copy(musics = musics.replacing(place, musics[place].copy(name = name, modifiedAt = at.toFileTime())))
    }

    /** The music of [id] played during the games, or none if [id] is null. */
    fun choosingForGames(id: ScoreId.Personal?): MusicLibraryContent {
        id?.let(::placeOf)
        return copy(gameMusicId = id)
    }

    /** The music of [id] taken out, no longer played during the games, and what [restoring] needs to put it back. */
    fun deleting(id: ScoreId.Personal): Pair<MusicLibraryContent, DeletedMusic?> {
        val place = musics.indexOfFirst { it.id == id }
        if (place < 0) return this to null
        val wasGameMusic = gameMusicId == id
        val deleted = DeletedMusic(musics[place], place, wasGameMusic)
        val left = copy(
            musics = musics.filterIndexed { index, _ -> index != place },
            gameMusicId = gameMusicId.takeUnless { wasGameMusic },
        )
        return left to deleted
    }

    /**
     * The [deleted] music back at its place, at the end of the list at the most, played during the games again if it
     * was; unchanged if it is back already. Refused if another music took its name since.
     */
    fun restoring(deleted: DeletedMusic): MusicLibraryContent {
        if (musics.any { it.id == deleted.music.id }) return this
        requireFreeName(deleted.music.name, deleted.music.id)
        return copy(
            musics = musics.toMutableList().apply { add(deleted.place.coerceAtMost(size), deleted.music) },
            gameMusicId = if (deleted.wasGameMusic) deleted.music.id else gameMusicId,
        )
    }

    /** The place of the music of [id]; refused if none has it. */
    private fun placeOf(id: ScoreId.Personal): Int =
        musics.indexOfFirst { it.id == id }.also { require(it >= 0) { "No saved music is known by $id" } }

    private fun requireFreeName(
        name: String,
        apartFrom: ScoreId.Personal,
    ) {
        require(!musics.isNameTaken(name, apartFrom)) { "Another saved music is called \"$name\"" }
    }

    private fun List<SavedMusic>.replacing(
        place: Int,
        music: SavedMusic,
    ): List<SavedMusic> = toMutableList().apply { set(place, music) }
}
