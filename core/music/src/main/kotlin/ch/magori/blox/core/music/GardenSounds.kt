package ch.magori.blox.core.music

/**
 * The sound effects of the musical garden, apart from [SoundEffects] so that the game does not warm them up: the
 * [note] of each cell of the preview, played one after the other, [NOTE_STEP_MILLIS] apart, in a rising arpeggio. One
 * effect per pitch, whatever the place of the note in the arpeggio, so that the effects of the garden stay as many as
 * its pitches: a finite set, warmed up once and never released (ADR 0025, ADR 0027). A first setting, to be tuned by
 * ear on a phone (P10.17).
 */
public object GardenSounds {
    private const val NOTE_MILLIS = 100

    /** How long after a note of the preview the next one starts: the notes ring a little under each other. */
    public const val NOTE_STEP_MILLIS: Int = 80

    /** A note of the preview: [pitch], a blip of 100 ms on the square, as loud as the lines cleared. */
    public fun note(pitch: Pitch): SoundEffect = SoundEffect(
        Waveform.SQUARE,
        SoundEffects.linesCleared.volume,
        SQUARE_ENVELOPE,
        listOf(Blip(pitch, NOTE_MILLIS)),
    )
}
