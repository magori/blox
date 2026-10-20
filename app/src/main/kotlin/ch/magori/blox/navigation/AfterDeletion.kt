package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.feature.composer.navigation.ComposerRoute
import ch.magori.blox.feature.musics.navigation.MyMusicsRoute

/**
 * The editor on top of [backStack] closed on the deletion of its music, [deleted]: « Mes musiques » comes back, and
 * [handOver] gives it the music (`DeletedMusicRelay.handOver`), so that it offers « Annuler ».
 *
 * Opened from the list, the editor leaves it on top. Opened from home (a new music saved, then deleted), the list takes
 * its place, over home: every deletion offers « Annuler ». Two stacks the application never builds stay sound: the
 * editor alone leaves the list alone; a list lower in the stack comes back on top, the screens above it leaving with
 * the editor, so that the stack never holds the same key twice. Once the editor is no longer on top (a late second
 * exit), nothing changes and nothing is handed over: no deletion waits for a list opened later.
 */
internal fun afterDeletion(
    backStack: MutableList<NavKey>,
    deleted: DeletedMusic,
    handOver: (DeletedMusic) -> Unit,
) {
    if (backStack.lastOrNull() !is ComposerRoute) return
    val list = backStack.lastIndexOf(MyMusicsRoute)
    if (list >= 0) {
        backStack.subList(list + 1, backStack.size).clear()
    } else {
        backStack.removeAt(backStack.lastIndex)
        backStack.add(MyMusicsRoute)
    }
    handOver(deleted)
}
