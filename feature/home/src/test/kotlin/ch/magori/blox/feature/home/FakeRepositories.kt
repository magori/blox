package ch.magori.blox.feature.home

import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.RecordResult
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import java.io.IOException

/**
 * Settings kept in memory. With [initial] null, nothing is read yet: the flow emits on the first
 * [store]. With [failWrites], every write fails as a full disk would, and nothing changes.
 */
class FakeUserPreferencesRepository(
    initial: UserPreferences? = UserPreferences.DEFAULT,
    private val failWrites: Boolean = false,
) : UserPreferencesRepository {
    private val stored = MutableStateFlow(initial)

    override val preferences: Flow<UserPreferences> = stored.filterNotNull()

    /** The settings saved so far, or null if nothing was read yet. */
    val saved: UserPreferences? get() = stored.value

    /** The settings are read, or changed, outside of the home screen. */
    fun store(preferences: UserPreferences) {
        stored.value = preferences
    }

    override suspend fun setTheme(theme: ThemeChoice) = write { it.copy(theme = theme) }

    override suspend fun setMusic(music: MusicChoice) = write { it.copy(music = music) }

    override suspend fun setStartLevel(startLevel: Int) = write { it.copy(startLevel = startLevel) }

    private fun write(change: (UserPreferences) -> UserPreferences) {
        if (failWrites) throw IOException("The disk is full")
        stored.value = change(stored.value ?: UserPreferences.DEFAULT)
    }
}

/** A record kept in memory; [reads], when given, is what the record flow gives instead, for reads that go wrong. */
class FakeHighScoreRepository(initial: Int = 0, reads: Flow<Int>? = null) : HighScoreRepository {
    private val record = MutableStateFlow(initial)

    override val highScore: Flow<Int> = reads ?: record

    override suspend fun submit(score: Int): RecordResult {
        val previous = record.value
        if (score > previous) record.value = score
        return RecordResult(previous, isNewRecord = score > previous)
    }
}
