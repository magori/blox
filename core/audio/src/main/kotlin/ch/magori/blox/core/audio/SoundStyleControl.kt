package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundStyle

/**
 * What plays the scores in a [SoundStyle] of the settings, « Instruments »: the music players ([Player]). A style set
 * applies from the next music played (the next play, resume or switch), never to the music playing, which goes on
 * as it was rendered; until another one is set, it holds for every music played. A music file plays as it is.
 */
public interface SoundStyleControl {
    /** Plays the scores in [style] from the next music played. Starts at [SoundStyle.ARCADE]. */
    public fun setSoundStyle(style: SoundStyle)
}
