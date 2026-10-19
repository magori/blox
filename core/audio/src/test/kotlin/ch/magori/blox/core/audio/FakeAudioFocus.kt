package ch.magori.blox.core.audio

/**
 * An audio focus that notes what it is asked in [log] (`focus request`, `focus abandon`), grants it
 * when [granted], and lets the test [send] what the system would tell the holder of the focus.
 */
internal class FakeAudioFocus(private val log: MutableList<String>) : AudioFocus {
    var granted = true
    private var onEvent: ((FocusEvent) -> Unit)? = null

    override fun request(onEvent: (FocusEvent) -> Unit): Boolean {
        log += "focus request"
        if (granted) this.onEvent = onEvent
        return granted
    }

    override fun abandon() {
        log += "focus abandon"
        onEvent = null
    }

    /** What the system tells the holder of the focus; nobody hears it once the focus is abandoned. */
    fun send(event: FocusEvent) {
        onEvent?.invoke(event)
    }
}
