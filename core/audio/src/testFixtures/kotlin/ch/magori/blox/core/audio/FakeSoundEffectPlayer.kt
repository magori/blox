package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundEffect

/** A player of sound effects that plays nothing and notes what it is asked to warm up and to play, in order. */
class FakeSoundEffectPlayer : SoundEffectPlayer {
    /** The effects asked to be warmed up, in order. */
    val warmedUp = mutableListOf<SoundEffect>()

    /** The effects asked for, in order. */
    val played = mutableListOf<SoundEffect>()

    override fun warmUp(effects: List<SoundEffect>) {
        warmedUp += effects
    }

    override fun play(effect: SoundEffect) {
        played += effect
    }
}
