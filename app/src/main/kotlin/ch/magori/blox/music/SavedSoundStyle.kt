package ch.magori.blox.music

import ch.magori.blox.core.audio.SoundStyleControl
import ch.magori.blox.core.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Applies the sound style saved in [preferences] to [music], as soon as it is read, then at each change, in this
 * scope, for its whole life: the next music played takes it (ADR 0032). It is applied only when it changes, not when
 * another setting does.
 */
internal fun CoroutineScope.applySavedSoundStyle(
    preferences: UserPreferencesRepository,
    music: SoundStyleControl,
) {
    launch { preferences.preferences.map { it.soundStyle }.distinctUntilChanged().collect(music::setSoundStyle) }
}
