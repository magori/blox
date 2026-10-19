package ch.magori.blox.feature.game

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update

/** Settings kept in memory, Korobeiniki by default; a null [state] is settings that have not answered yet. */
internal class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences.DEFAULT) :
    UserPreferencesRepository {
    val state = MutableStateFlow<UserPreferences?>(initial)

    /** What [preferences] gives: [state], unless the test sets another flow before the view model reads it. */
    var preferencesFlow: Flow<UserPreferences> = state.filterNotNull()

    override val preferences: Flow<UserPreferences> get() = preferencesFlow

    override suspend fun setTheme(theme: ThemeChoice) {
        state.update { it?.copy(theme = theme) }
    }

    override suspend fun setMusic(music: MusicChoice) {
        state.update { it?.copy(music = music) }
    }

    override suspend fun setStartLevel(startLevel: Int) {
        state.update { it?.copy(startLevel = startLevel) }
    }
}
