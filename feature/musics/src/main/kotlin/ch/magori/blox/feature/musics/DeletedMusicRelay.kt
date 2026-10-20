package ch.magori.blox.feature.musics

import ch.magori.blox.core.data.DeletedMusic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Hands a music deleted on another screen, the editor, over to « Mes musiques », which offers to undo it as for a
 * deletion of its own. `app` builds a single one for the process, gives it to the [MyMusicsViewModel.Factory], and
 * fills it as the editor closes on a deletion, the list then on top: the editor is another feature, that the list does
 * not know. It lives in memory only: a process that dies before the list takes it loses the chance to undo, nothing
 * more.
 */
class DeletedMusicRelay {
    /** The music handed over and not taken yet; null when there is none. */
    private val pending = MutableStateFlow<DeletedMusic?>(null)

    /** Hands [deleted] over to the list, in place of one it did not take. */
    fun handOver(deleted: DeletedMusic) {
        pending.value = deleted
    }

    /** Takes [deleted] from the relay, for one list only: false if another took it, or another replaced it. */
    internal fun take(deleted: DeletedMusic): Boolean = pending.compareAndSet(deleted, null)

    /** The musics handed over, as long as none has been taken. */
    internal val handedOver: Flow<DeletedMusic> = pending.filterNotNull()
}
