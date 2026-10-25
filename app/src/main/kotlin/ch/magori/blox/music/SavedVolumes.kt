package ch.magori.blox.music

import ch.magori.blox.core.audio.VolumeControl
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Applies the volumes saved in [preferences], that of the music to [music] and that of the sound effects to [effects],
 * as soon as they are read, then at each change, in this scope, for its whole life: what plays, a preview or a game,
 * changes at once. Each one is applied only when it changes, not when another setting does.
 */
internal fun CoroutineScope.applySavedVolumes(
    preferences: UserPreferencesRepository,
    music: VolumeControl,
    effects: VolumeControl,
) {
    follow(preferences, UserPreferences::musicVolume, music)
    follow(preferences, UserPreferences::effectsVolume, effects)
}

/** Applies to [control] the [volume] of the settings saved in [preferences], at each change. */
private fun CoroutineScope.follow(
    preferences: UserPreferencesRepository,
    volume: (UserPreferences) -> Volume,
    control: VolumeControl,
) {
    launch { preferences.preferences.map(volume).distinctUntilChanged().collect(control::setVolume) }
}
