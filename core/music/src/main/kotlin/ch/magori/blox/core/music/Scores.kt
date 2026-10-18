package ch.magori.blox.core.music

/**
 * The BloX arrangements of public domain melodies, transcribed bar by bar from docs/musique.md.
 * Durations are in beats (1 = quarter note, 0.5 = eighth note).
 */
public object Scores {
    private const val MELODY_VOLUME = 0.07
    private const val BASS_VOLUME = 0.16

    public val korobeiniki: Score = Score(
        id = ScoreId.KOROBEINIKI,
        bpm = 150,
        beatsPerBar = 4,
        voices = listOf(
            melody(
                bar("E4" to 1.5, "G#4" to 0.5, "B4" to 1.0, "G#4" to 0.5, "E4" to 0.5),
                bar("A4" to 1.5, "C5" to 0.5, "E5" to 1.0, "D5" to 0.5, "C5" to 0.5),
                bar("B4" to 1.5, "C5" to 0.5, "D5" to 1.0, "E5" to 1.0),
                bar("C5" to 1.0, "A4" to 1.0, "A4" to 2.0),
                bar("F5" to 1.5, "G5" to 0.5, "A5" to 1.0, "G5" to 0.5, "F5" to 0.5),
                bar("E5" to 1.5, "F5" to 0.5, "E5" to 1.0, "D5" to 0.5, "C5" to 0.5),
                bar("B4" to 1.5, "C5" to 0.5, "D5" to 1.0, "E5" to 1.0),
                bar("C5" to 1.0, "A4" to 1.0, "A4" to 2.0),
            ),
            bass(
                rootAndFifth("E2", "B2"),
                rootAndFifth("A2", "E3"),
                rootAndFifth("E2", "B2"),
                rootAndFifth("A2", "E3"),
                rootAndFifth("D2", "A2"),
                rootAndFifth("A2", "E3"),
                rootAndFifth("E2", "B2"),
                rootAndFifth("A2", "E3"),
            ),
        ),
    )

    public val minuet: Score = Score(
        id = ScoreId.MINUET,
        bpm = 132,
        beatsPerBar = 3,
        voices = listOf(
            melody(
                bar("D5" to 1.0, "G4" to 0.5, "A4" to 0.5, "B4" to 0.5, "C5" to 0.5),
                bar("D5" to 1.0, "G4" to 1.0, "G4" to 1.0),
                bar("E5" to 1.0, "C5" to 0.5, "D5" to 0.5, "E5" to 0.5, "F#5" to 0.5),
                bar("G5" to 1.0, "G4" to 1.0, "G4" to 1.0),
                bar("C5" to 1.0, "D5" to 0.5, "C5" to 0.5, "B4" to 0.5, "A4" to 0.5),
                bar("B4" to 1.0, "C5" to 0.5, "B4" to 0.5, "A4" to 0.5, "G4" to 0.5),
                bar("F#4" to 1.0, "G4" to 0.5, "A4" to 0.5, "B4" to 0.5, "G4" to 0.5),
                bar("A4" to 3.0),
            ),
            bass(
                bar("G2" to 3.0),
                bar("B2" to 3.0),
                bar("C3" to 3.0),
                bar("B2" to 3.0),
                bar("A2" to 3.0),
                bar("G2" to 3.0),
                bar("D3" to 3.0),
                bar("D2" to 3.0),
            ),
        ),
    )

    private fun melody(vararg bars: List<Note>): Voice = Voice(Waveform.SQUARE, MELODY_VOLUME, bars.flatMap { it })

    private fun bass(vararg bars: List<Note>): Voice = Voice(Waveform.TRIANGLE, BASS_VOLUME, bars.flatMap { it })

    /** A bar of (pitch name, beats) pairs. */
    private fun bar(vararg notes: Pair<String, Double>): List<Note> =
        notes.map { (name, beats) -> Note(Pitch.parse(name), beats) }

    /** A bar of four quarter notes: root, fifth, root, fifth. */
    private fun rootAndFifth(
        root: String,
        fifth: String,
    ): List<Note> = bar(root to 1.0, fifth to 1.0, root to 1.0, fifth to 1.0)
}
