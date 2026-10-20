package ch.magori.blox.core.data

import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant

/**
 * Saved musics kept in memory, that keep the contract of [MusicLibrary] (`MusicLibraryContract`): the rules are
 * those of the real library, [MusicLibraryContent], only the file is left out. [clock] dates the changes: stopped at
 * [MODIFIED_AT] unless a test gives another one.
 */
class FakeMusicLibrary(private val clock: Clock = MutableClock()) : MusicLibrary {
    private val content = MutableStateFlow(MusicLibraryContent())

    /** The identities given so far: the next one is « music-<count + 1> », or the first after it no music has. */
    private var ids = 0

    override fun newId(): ScoreId.Personal {
        val saved = content.value.musics.map { it.id }.toSet()
        return generateSequence { ScoreId.Personal("music-${++ids}") }.first { it !in saved }
    }

    override val musics: Flow<List<SavedMusic>> = content.map { it.musics }.distinctUntilChanged()

    override val gameMusic: Flow<SavedMusic?> = content.map { it.gameMusic }.distinctUntilChanged()

    override suspend fun save(music: SavedMusic) {
        content.update { it.saving(music, clock.instant()) }
    }

    override suspend fun rename(
        id: ScoreId.Personal,
        name: String,
    ) {
        content.update { it.renaming(id, name, clock.instant()) }
    }

    override suspend fun setGameMusic(id: ScoreId.Personal?) {
        content.update { it.choosingForGames(id) }
    }

    override suspend fun delete(id: ScoreId.Personal): DeletedMusic? {
        var deleted: DeletedMusic? = null
        content.update { current -> current.deleting(id).also { deleted = it.second }.first }
        return deleted
    }

    override suspend fun restore(deleted: DeletedMusic) {
        content.update { it.restoring(deleted) }
    }
}

/** The time the clock of a [FakeMusicLibrary] tells, unless it is given another one: the time its musics are saved. */
val MODIFIED_AT: Instant = Instant.parse("2026-10-20T10:00:00Z")
