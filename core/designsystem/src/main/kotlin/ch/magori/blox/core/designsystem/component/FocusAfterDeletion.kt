package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Passes the keyboard focus on once an element of a list is deleted from its window: once the window of the deletion
 * of [deleting] has closed and [deleting] is no longer among the keys [listed], in whichever order the two come, the
 * focus goes to the element listed after it ([focusOf]), else before it, else to the [empty] list; in the order of
 * [listed] as the window opens (or opens again, after a deletion not confirmed), kept even if the list is read again
 * without the element while the window is still open. The list gives the focus, not the element that leaves, which
 * takes its own focus away with it. The deletion is awaited until the focus is asked: an effect cancelled during the
 * frame it waits, the list or the window changing, asks it again as it restarts.
 *
 * A deletion not confirmed, the element still listed, waits for it to leave until another window opens
 * ([windowOpen]). By touch, nothing is focused. The focus is asked a frame later, once the other effects of the
 * closing, those of the element leaving among them, have run. [reveal] is first given the index in [listed] of the
 * element that takes the focus, even when it is already in sight: a lazy list that has not composed it brings it in.
 */
@Composable
fun <K : Any> FocusAfterDeletion(
    listed: List<K>,
    deleting: K?,
    windowOpen: Boolean,
    focusOf: (K) -> FocusRequester?,
    empty: FocusRequester,
    reveal: suspend (index: Int) -> Unit = {},
) {
    val inputMode = LocalInputModeManager.current
    // The last ones given, read by the effect without restarting it (named apart: a parameter may not be shadowed).
    val currentFocusOf by rememberUpdatedState(focusOf)
    val currentReveal by rememberUpdatedState(reveal)
    val pending = remember { PendingDeletion<K>() }
    LaunchedEffect(listed, windowOpen, deleting) {
        when {
            deleting != null -> pending.waitFor(deleting, listed)

            windowOpen -> pending.waitFor(null, emptyList())

            else -> pending.left(listed)?.let { target ->
                if (inputMode.inputMode == InputMode.Keyboard) {
                    withFrameNanos {}
                    when (target) {
                        is FocusTarget.Element -> {
                            currentReveal(listed.indexOf(target.key))
                            currentFocusOf(target.key)?.requestFocus()
                        }

                        FocusTarget.EmptyList -> empty.requestFocus()
                    }
                }
                // Only now: an effect cancelled before, as the list or the window changes, finds it again as it restarts.
                pending.given()
            }
        }
    }
}

/** The element whose deletion the list waits to see, [key], and the [order] of the list as its window opened. */
internal class PendingDeletion<K : Any> {
    private var key: K? = null
    private var order: List<K> = emptyList()

    /**
     * The window of the deletion of [key] is open, the list in [order]; null for another window. The order is the one
     * as the window opens, or opens again; a list read again for the same key without it does not replace it.
     */
    fun waitFor(
        key: K?,
        order: List<K>,
    ) {
        if (key != null && key == this.key && key !in order) return
        this.key = key
        this.order = order
    }

    /** The focus given after the deletion awaited: it is no longer awaited. */
    fun given() {
        key = null
    }

    /**
     * Once the element awaited is no longer [listed], where the focus goes: the element listed after it, else before
     * it, else the empty list; null while nothing has left. The same, until it is [given]. An element missing from the
     * order gives it to the first element listed.
     */
    fun left(listed: List<K>): FocusTarget<K>? {
        val gone = key?.takeUnless { it in listed } ?: return null
        val at = order.indexOf(gone)
        val stays = if (at < 0) {
            listed.firstOrNull()
        } else {
            order.drop(at + 1).firstOrNull { it in listed } ?: order.take(at).lastOrNull { it in listed }
        }
        return stays?.let { FocusTarget.Element(it) } ?: FocusTarget.EmptyList
    }
}

/** Where the focus goes after a deletion. */
internal sealed interface FocusTarget<out K> {
    /** The element of [key]. */
    data class Element<K>(val key: K) : FocusTarget<K>

    /** The list left empty. */
    data object EmptyList : FocusTarget<Nothing>
}
