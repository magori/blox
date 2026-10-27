package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import java.io.IOException

/** The fake shared by the tests of the features keeps the contract of the real results of the day. */
class FakeDailyChallengeRepositoryTest : DailyChallengeRepositoryContract() {
    override fun emptyRepository(scope: CoroutineScope): DailyChallengeRepository = FakeDailyChallengeRepository()

    override suspend fun unwritableRepository(scope: CoroutineScope): DailyChallengeRepository =
        FakeDailyChallengeRepository(initial = DailyResults(date = OCTOBER_26, tries = 1))
            .apply { failure = IOException("disk full") }
}
