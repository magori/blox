package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.BASS_RANGE
import ch.magori.blox.core.music.MELODY_RANGE
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.PitchRange
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.bassVoice
import ch.magori.blox.core.music.melodyVoice

/**
 * A track of the editor, edited on its own tab, whose grid spans [range]: the two octaves in which the generator writes
 * that voice. Its notes play as [voice] makes them play: on the wave and at the volume of that voice in every score of
 * BloX.
 */
internal enum class Track(val range: PitchRange, val voice: (List<Note>) -> Voice) {
    MELODY(MELODY_RANGE, ::melodyVoice),
    BASS(BASS_RANGE, ::bassVoice),
    ;

    /** The track of the other tab: the bass for the melody, the melody for the bass. */
    val other: Track get() = if (this == MELODY) BASS else MELODY
}
