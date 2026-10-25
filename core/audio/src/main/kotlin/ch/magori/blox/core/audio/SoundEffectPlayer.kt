package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundEffect

/**
 * Plays the short sound effects of the game, over the music, without stopping it nor asking for the audio focus.
 * [SoundEffectTracks] plays them on the device; whether the player wants them at all is decided by the caller,
 * before it asks. The effects play at the volume set ([VolumeControl]), apart from the volume of the music.
 */
public interface SoundEffectPlayer : VolumeControl {
    /**
     * Gets [effects] ready to play, without playing them, so that their first [play] waits for nothing: a game warms
     * up its effects as soon as it knows the player wants them. Returns at once; never fails.
     */
    public fun warmUp(effects: List<SoundEffect>)

    /**
     * Plays [effect] from its start, as soon as it can, without waiting for it. Never fails: an audio output
     * that fails plays nothing.
     */
    public fun play(effect: SoundEffect)
}
