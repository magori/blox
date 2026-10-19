package ch.magori.blox.feature.musics

import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

/**
 * The fake library of core:data, whose writes of the [broken] kinds fail with an [IOException], as on a full disk:
 * nothing is written then. Its readings go on. A deletion waits for [deletionHeld], when there is one, as a slow disk.
 */
internal class BreakableMusicLibrary(private val fake: FakeMusicLibrary = FakeMusicLibrary()) : MusicLibrary by fake {
    /** The writes of a library. */
    enum class Write { SAVE, RENAME, SET_GAME_MUSIC, DELETE, RESTORE }

    var broken: Set<Write> = emptySet()

    /** Once set, a deletion waits for it to complete before it is written. */
    var deletionHeld: CompletableDeferred<Unit>? = null

    override suspend fun save(music: SavedMusic) = unless(Write.SAVE) { fake.save(music) }

    override suspend fun rename(
        id: ScoreId.Personal,
        name: String,
    ) = unless(Write.RENAME) { fake.rename(id, name) }

    override suspend fun setGameMusic(id: ScoreId.Personal?) = unless(Write.SET_GAME_MUSIC) { fake.setGameMusic(id) }

    override suspend fun delete(id: ScoreId.Personal): DeletedMusic? {
        deletionHeld?.await()
        return unless(Write.DELETE) { fake.delete(id) }
    }

    override suspend fun restore(deleted: DeletedMusic) = unless(Write.RESTORE) { fake.restore(deleted) }

    private inline fun <T> unless(
        write: Write,
        writing: () -> T,
    ): T {
        if (write in broken) throw IOException("The disk is full")
        return writing()
    }
}
