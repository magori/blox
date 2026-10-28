package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * Runs [block] in a scope of its own, on the scheduler of the test, then closes it: the DataStores living in it release
 * their file, which a new repository may then open.
 */
internal suspend fun <T> TestScope.inScopeClosedAfter(block: suspend (CoroutineScope) -> T): T {
    val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
    val result = block(scope)
    scope.coroutineContext.job.cancelAndJoin()
    return result
}
