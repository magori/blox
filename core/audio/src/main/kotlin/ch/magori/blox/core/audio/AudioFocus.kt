package ch.magori.blox.core.audio

/**
 * The audio focus of the system, for the music: one app plays at a time. [FocusedMusicPlayer] asks
 * for it before playing; [SystemAudioFocus] asks `AudioManager`. The holder of the focus also hears
 * when the headphones are unplugged ([FocusEvent.BECOMING_NOISY]).
 */
public interface AudioFocus {
    /**
     * Asks for the focus for as long as the music plays; whether it is granted. Once granted, [onEvent]
     * hears what happens to it, on the main thread, until [abandon].
     */
    public fun request(onEvent: (FocusEvent) -> Unit): Boolean

    /** Gives the focus back: the music no longer plays. */
    public fun abandon()
}

/** What happens to the focus while the music holds it. */
public enum class FocusEvent {
    /** Another app took the focus for good, as a podcast, or a music player. */
    LOSS,

    /** Another app took the focus for a while, as a voice assistant, or a navigation voice. */
    TRANSIENT_LOSS,

    /** The focus came back after a [TRANSIENT_LOSS]. */
    GAIN,

    /** The sound is about to go from the headphones, unplugged, to the speaker. */
    BECOMING_NOISY,
}
