package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope

/** The fake shared by the tests of the features keeps the contract of the real settings. */
class FakeUserPreferencesRepositoryTest : UserPreferencesRepositoryContract() {
    override fun emptyRepository(scope: CoroutineScope): UserPreferencesRepository = FakeUserPreferencesRepository()
}
