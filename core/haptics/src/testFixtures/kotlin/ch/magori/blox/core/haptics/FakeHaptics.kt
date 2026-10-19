package ch.magori.blox.core.haptics

/** Haptic feedback that vibrates nothing and notes what it is asked to play, in order. */
class FakeHaptics : Haptics {
    /** The haptics asked for, in order. */
    val played = mutableListOf<Haptic>()

    override fun play(haptic: Haptic) {
        played += haptic
    }
}
