package ch.magori.blox.feature.home

import ch.magori.blox.core.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Saves a choice in [preferences], in this scope. A write that fails (a full disk) is dropped: the screen keeps showing
 * the saved settings, and the player may choose again.
 */
internal fun CoroutineScope.saveSettings(
    preferences: UserPreferencesRepository,
    write: suspend UserPreferencesRepository.() -> Unit,
) {
    launch {
        try {
            preferences.write()
        } catch (_: IOException) {
            // Nothing saved: the settings shown are still the saved ones.
        }
    }
}
