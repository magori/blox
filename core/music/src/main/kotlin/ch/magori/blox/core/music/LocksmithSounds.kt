package ch.magori.blox.core.music

/**
 * The sound effects of the Locksmith, apart from [SoundEffects] so that the game does not warm them up: after each
 * placement, the [effectsOf] its chord, an arpeggio and a bass played together on two tracks; the [openingFlourish]
 * once the padlock is open; the [refusal] of a placement. Their volumes are read on [SoundEffects], so that they keep
 * its rule: the square a little above the melodies of the scores, the triangle louder, its low register softer.
 */
public object LocksmithSounds {
    private const val ARPEGGIO_BLIP_MILLIS = 70
    private const val ARPEGGIO_HELD_MILLIS = 300

    /** A triad: two blips, then the fifth held. */
    private const val ARPEGGIO_MILLIS = 2 * ARPEGGIO_BLIP_MILLIS + ARPEGGIO_HELD_MILLIS
    private const val BASS_OCTAVES_DOWN = 2

    /**
     * The root, the third and the fifth of [chord], upwards, in blips of 70 ms, the fifth held 300 ms: 440 ms in all,
     * on the square, as loud as the lines cleared.
     */
    public fun arpeggio(chord: Chord): SoundEffect {
        val pitches = chord.pitches
        return SoundEffect(
            Waveform.SQUARE,
            SoundEffects.linesCleared.volume,
            SQUARE_ENVELOPE,
            pitches.dropLast(1).map { Blip(it, ARPEGGIO_BLIP_MILLIS) } + Blip(pitches.last(), ARPEGGIO_HELD_MILLIS),
        )
    }

    /**
     * The root of [chord] two octaves down, held as long as its whole [arpeggio], on the triangle, as loud as the game
     * over. Refused with an `IllegalArgumentException` if that note falls out of [BASS_RANGE], C2 to C4: the root of
     * [chord] goes from C4 to C6.
     */
    public fun bass(chord: Chord): SoundEffect {
        val root = chord.root.midi - BASS_OCTAVES_DOWN * SEMITONES_PER_OCTAVE
        require(root in BASS_RANGE.lowest.midi..BASS_RANGE.highest.midi) {
            "The bass of a chord, two octaves below its root ${chord.root}, stays from C2 to C4"
        }
        return SoundEffect(
            Waveform.TRIANGLE,
            SoundEffects.gameOver.volume,
            TRIANGLE_ENVELOPE,
            listOf(Blip(Pitch(root), ARPEGGIO_MILLIS)),
        )
    }

    /** The [arpeggio] of [chord], then its [bass]: two effects, played together on two tracks. */
    public fun effectsOf(chord: Chord): List<SoundEffect> = listOf(arpeggio(chord), bass(chord))

    /** The padlock is open: the chord of C climbs from C5 to C6, the last held, as loud as the four lines cleared. */
    public val openingFlourish: SoundEffect = SoundEffect(
        Waveform.SQUARE,
        SoundEffects.fourLinesCleared.volume,
        SQUARE_ENVELOPE,
        blipsOf("C5" to 60, "E5" to 60, "G5" to 60, "C6" to 240),
    )

    /**
     * A placement refused: a soft buzz falling a semitone, C3 then B2, in 180 ms, on the square, a little louder than
     * the rotation only.
     */
    public val refusal: SoundEffect = SoundEffect(
        Waveform.SQUARE,
        volume = 0.06,
        SQUARE_ENVELOPE,
        blipsOf("C3" to 90, "B2" to 90),
    )
}
