package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Saves a choice in these preferences: launches [write] in [scope], without waiting, then calls [onSaved] once the
 * write is done. A write that fails with an [IOException] (a full disk) is dropped: [onSaved] is not called, nothing is
 * thrown, and the settings read stay the saved ones. A write cancelled (the scope closed) is not swallowed: the
 * cancellation goes on, and [onSaved] is not called. [onSaved] is called only after a write that succeeded.
 */
fun UserPreferencesRepository.saveSettings(
    scope: CoroutineScope,
    write: suspend UserPreferencesRepository.() -> Unit,
    onSaved: () -> Unit = {},
) {
    scope.launch {
        try {
            write()
        } catch (_: IOException) {
            // Nothing saved: the settings shown are still the saved ones.
            return@launch
        }
        onSaved()
    }
}
