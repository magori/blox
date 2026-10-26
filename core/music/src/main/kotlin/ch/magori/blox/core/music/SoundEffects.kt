package ch.magori.blox.core.music

/** The envelope of the blips of the effects on the square: a rise of 2 ms, a fall of 15 ms. */
internal val SQUARE_ENVELOPE: Envelope = Envelope(attackMillis = 2, releaseMillis = 15)

/** The envelope of the blips of the effects on the triangle, longer and lower: a rise of 2 ms, a fall of 30 ms. */
internal val TRIANGLE_ENVELOPE: Envelope = Envelope(attackMillis = 2, releaseMillis = 30)

/**
 * The sound effects of BloX: short chiptune beeps written for BloX, plain notes of the chord of C, played over the
 * music without stopping it. From a few tens to a few hundred milliseconds; the square is a little louder than the
 * melodies of the scores (0.07), so that an effect is heard over them, the triangle as loud as their basses (0.16)
 * and a little more, its low register sounding softer. Each blip rises and falls over a few milliseconds, so that
 * none clicks.
 */
public object SoundEffects {
    /** A piece turned: a single short tick, the quietest effect, since it comes several times a second. */
    public val rotation: SoundEffect = SoundEffect(
        Waveform.SQUARE,
        volume = 0.05,
        Envelope(attackMillis = 1, releaseMillis = 10),
        blipsOf("E6" to 30),
    )

    /** One to three lines cleared: a quick rise up the chord, its last note held a little. */
    public val linesCleared: SoundEffect = SoundEffect(
        Waveform.SQUARE,
        volume = 0.08,
        SQUARE_ENVELOPE,
        blipsOf("C6" to 40, "E6" to 40, "G6" to 80),
    )

    /** Four lines cleared at once, the best move: the same rise, over two octaves, louder, ending on a held C7. */
    public val fourLinesCleared: SoundEffect = SoundEffect(
        Waveform.SQUARE,
        volume = 0.09,
        SQUARE_ENVELOPE,
        blipsOf("C5" to 50, "E5" to 50, "G5" to 50, "C6" to 50, "E6" to 50, "G6" to 50, "C7" to 150),
    )

    /** The game is over: the chord falls, slower, on the softer triangle, down to a held G3. */
    public val gameOver: SoundEffect = SoundEffect(
        Waveform.TRIANGLE,
        volume = 0.2,
        TRIANGLE_ENVELOPE,
        blipsOf("G4" to 120, "E4" to 120, "C4" to 120, "G3" to 240),
    )

    /**
     * The level goes up: a call of four octave leaps on C, C6 then C7, twice, the last C7 held, as loud as the lines
     * cleared; leaps rather than a rise up the chord, so that it is not heard as one more line cleared, and shorter
     * than the four lines.
     */
    public val levelUp: SoundEffect = SoundEffect(
        Waveform.SQUARE,
        volume = 0.08,
        SQUARE_ENVELOPE,
        blipsOf("C6" to 50, "C7" to 50, "C6" to 50, "C7" to 150),
    )

    /** Every effect, from the lightest to the heaviest. */
    public val all: List<SoundEffect> = listOf(rotation, linesCleared, levelUp, fourLinesCleared, gameOver)
}
