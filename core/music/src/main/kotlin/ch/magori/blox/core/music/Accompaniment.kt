package ch.magori.blox.core.music

/**
 * The accompaniment of Au tempo, its music: a bass on I–V–vi–IV of C major, one chord a bar, and a metronome that
 * clicks on every quarter, louder and higher on beat 1, a phrase of 4 bars of 4 beats in a loop. Written at 100 beats
 * per minute and played at the tempo of the game by [tempoFactor]. A first setting, to be tuned by ear on a phone
 * (P11.10).
 */
public object Accompaniment {
    /** The tempo the accompaniment is written at, in beats per minute: 18 ticks per eighth. */
    private const val BPM = 100

    /** The ticks of a minute, a tick lasting 1/60 s (ADR 0006). */
    private const val TICKS_PER_MINUTE = 3600
    private const val EIGHTHS_PER_BEAT = 2
    private const val BEATS_PER_BAR = 4
    private const val BEAT = 1.0
    private const val EIGHTH = 0.5
    private const val CLICK = 0.125
    private const val BEAT_1_VOLUME = 0.10
    private const val OTHER_BEATS_VOLUME = 0.05

    /**
     * The identity of the accompaniment, the same with or without the metronome: switching it off keeps the same music,
     * without its clicks. Like `LiveMusic.CALM_ID` and the score of a garden, it names no music of the library: the
     * accompaniment is never kept as a music of the player.
     */
    private val ID = ScoreId.Personal("tempo-accompaniment")
    private val SCALE = Scale(Pitch.parse("C3"), ScaleType.MAJOR)
    private val PROGRESSION = Progression.parse("I-V-vi-IV")
    private val BARS = PROGRESSION.numerals.indices

    /** The bass plays its roots from F2 up to E3, so that C3 goes down to G2, A2 and F2. */
    private val LOWEST_ROOT = Pitch.parse("F2")

    /** The root of the chord of each bar, on every eighth: the eighths the piece falls on. */
    private val BASS = bassVoice(
        BARS.flatMap { bar -> List(BEATS_PER_BAR * EIGHTHS_PER_BEAT) { Note(rootOf(bar), EIGHTH) } },
    )

    /**
     * A high C on beat 1 of every bar. Like every voice of the accompaniment, it starts a note or a rest on every
     * beat: the player switches or resumes a music only on the beats where every voice starts one.
     */
    private val BEAT_1_CLICKS = Voice(
        Waveform.SQUARE,
        BEAT_1_VOLUME,
        BARS.flatMap { click(Pitch.parse("C6")) + List(BEATS_PER_BAR - 1) { Note(null, BEAT) } },
    )

    /** A G below it, softer, on beats 2 to 4. */
    private val OTHER_BEATS_CLICKS = Voice(
        Waveform.SQUARE,
        OTHER_BEATS_VOLUME,
        BARS.flatMap { listOf(Note(null, BEAT)) + List(BEATS_PER_BAR - 1) { click(Pitch.parse("G5")) }.flatten() },
    )

    private val WITH_METRONOME = Score(ID, BPM, BEATS_PER_BAR, listOf(BASS, BEAT_1_CLICKS, OTHER_BEATS_CLICKS))
    private val WITHOUT_METRONOME = WITH_METRONOME.copy(voices = listOf(BASS))

    /** The accompaniment, with the clicks of the [metronome] or the bass alone. */
    public fun score(metronome: Boolean): Score = if (metronome) WITH_METRONOME else WITHOUT_METRONOME

    /**
     * The tempo factor that plays the accompaniment at the tempo of [ticksPerEighth], the ticks of an eighth of the
     * game: 18 / [ticksPerEighth], 1.2 at 15 ticks (120 beats per minute). Refused with an `IllegalArgumentException`
     * for a [ticksPerEighth] that is not positive.
     */
    public fun tempoFactor(ticksPerEighth: Int): Double {
        require(ticksPerEighth > 0) { "An eighth lasts a positive number of ticks, was $ticksPerEighth" }
        val beatsPerMinute = TICKS_PER_MINUTE.toDouble() / (EIGHTHS_PER_BEAT * ticksPerEighth)
        return beatsPerMinute / BPM
    }

    /** A beat that starts with a click of [pitch], then rests. */
    private fun click(pitch: Pitch): List<Note> = listOf(Note(pitch, CLICK), Note(null, BEAT - CLICK))

    private fun rootOf(bar: Int): Pitch = PROGRESSION.chordAt(bar, SCALE).root.inOctaveFrom(LOWEST_ROOT)
}
