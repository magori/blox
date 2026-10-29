package ch.magori.blox.feature.locksmith

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hands the progress cleared on the screen « Serrures » over to the padlock shown next, padlock 1, which tells it.
 * `app` builds a single one for the process and gives it to both factories. It lives in memory only: a process that
 * dies before padlock 1 takes it loses the message, nothing more.
 */
class ClearedProgressRelay {
    /** Whether a clear was handed over and not taken yet. */
    private val pending = MutableStateFlow(false)

    /** Hands a clear over to the padlock shown next. */
    internal fun handOver() {
        pending.value = true
    }

    /** Takes the clear handed over, for one padlock only: false if there is none, or another took it. */
    internal fun take(): Boolean = pending.compareAndSet(expect = true, update = false)

    /** Forgets a clear that no padlock took, as the screen of the padlocks opens: no later padlock starts over for it. */
    internal fun forget() {
        pending.value = false
    }
}
