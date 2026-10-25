package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.Volume

/**
 * A player of sound effects that plays nothing, notes what it is asked to warm up and to play, in order, and keeps
 * the [volume] set.
 */
class FakeSoundEffectPlayer : SoundEffectPlayer {
    /** The effects asked to be warmed up, in order. */
    val warmedUp = mutableListOf<SoundEffect>()

    /** The effects asked for, in order. */
    val played = mutableListOf<SoundEffect>()

    /** The volume the effects play at. */
    var volume: Volume = Volume.FULL
        private set

    /** Every volume set, in order, the same one set again included. */
    val volumesSet = mutableListOf<Volume>()

    override fun warmUp(effects: List<SoundEffect>) {
        warmedUp += effects
    }

    override fun play(effect: SoundEffect) {
        played += effect
    }

    override fun setVolume(volume: Volume) {
        this.volume = volume
        volumesSet += volume
    }
}
