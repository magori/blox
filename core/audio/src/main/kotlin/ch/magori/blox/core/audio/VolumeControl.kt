package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Volume

/**
 * What plays at a volume of the settings: the music ([Player], [MusicFilePlayer]) and the sound effects
 * ([SoundEffectPlayer]). The volume applies at once to what plays, and to what plays next, until another one is set:
 * a play does not set it back. A volume of 0 plays in silence: the music goes on, unheard. The players turn it into
 * the gain of their samples with [Volume.gain].
 */
public interface VolumeControl {
    /** Plays at [volume] from now on, what plays now included. Starts at [Volume.FULL]. */
    public fun setVolume(volume: Volume)
}
