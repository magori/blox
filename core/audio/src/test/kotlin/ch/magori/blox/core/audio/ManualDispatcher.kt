package ch.magori.blox.core.audio

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/** Runs its work only when the test says so: a rendering not ready yet is a task left in it. */
internal class ManualDispatcher : CoroutineDispatcher() {
    private val tasks = ArrayDeque<Runnable>()
    var running = false
        private set

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        tasks.addLast(block)
    }

    /** Runs the work waiting, renderings included. */
    fun runAll() {
        while (tasks.isNotEmpty()) {
            running = true
            try {
                tasks.removeFirst().run()
            } finally {
                running = false
            }
        }
    }
}
