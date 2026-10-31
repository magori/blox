package ch.magori.blox.core.data

import ch.magori.blox.core.music.SoundStyle
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import java.io.IOException

/**
 * Settings kept in memory, [UserPreferences.DEFAULT] unless [initial] says otherwise. A null [state] is
 * settings that have not answered yet: the flow emits on the first value. With [failWrites], every write
 * fails as a full disk would, and nothing changes. As the real one (`UserPreferencesRepositoryContract`), it
 * refuses a negative start level.
 */
class FakeUserPreferencesRepository(
    initial: UserPreferences? = UserPreferences.DEFAULT,
    private val failWrites: Boolean = false,
) : UserPreferencesRepository {
    /** The settings kept, or null if nothing was read yet; the test may change them, as another screen would. */
    val state = MutableStateFlow(initial)

    /** What [preferences] gives: [state], unless the test sets another flow before it is read. */
    var preferencesFlow: Flow<UserPreferences> = state.filterNotNull()

    override val preferences: Flow<UserPreferences> get() = preferencesFlow

    override suspend fun setTheme(theme: ThemeChoice) = write { it.copy(theme = theme) }

    override suspend fun setMusic(music: MusicChoice) = write { it.copy(music = music) }

    override suspend fun setStartLevel(startLevel: Int) {
        require(startLevel >= 0) { "A start level cannot be negative: $startLevel" }
        write { it.copy(startLevel = startLevel) }
    }

    override suspend fun setVibrations(vibrations: Boolean) = write { it.copy(vibrations = vibrations) }

    override suspend fun setSoundEffects(soundEffects: Boolean) = write { it.copy(soundEffects = soundEffects) }

    override suspend fun setReducedEffects(reducedEffects: Boolean) = write { it.copy(reducedEffects = reducedEffects) }

    override suspend fun setLeftHanded(leftHanded: Boolean) = write { it.copy(leftHanded = leftHanded) }

    override suspend fun setKeyVibration(keyVibration: Boolean) = write { it.copy(keyVibration = keyVibration) }

    override suspend fun setMusicVolume(musicVolume: Volume) = write { it.copy(musicVolume = musicVolume) }

    override suspend fun setEffectsVolume(effectsVolume: Volume) = write { it.copy(effectsVolume = effectsVolume) }

    override suspend fun setSoundStyle(soundStyle: SoundStyle) = write { it.copy(soundStyle = soundStyle) }

    /** As DataStore does, a write applies to the settings kept, or to the default ones if there are none. */
    private fun write(change: (UserPreferences) -> UserPreferences) {
        if (failWrites) throw IOException("The disk is full")
        state.value = change(state.value ?: UserPreferences.DEFAULT)
    }
}
