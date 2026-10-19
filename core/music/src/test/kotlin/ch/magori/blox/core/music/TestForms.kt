package ch.magori.blox.core.music

/**
 * A form of joyful in [scale], at 120 beats per minute, of one bar for each of [chords], playing that chord all along a
 * whole note: the measures of a melody written for it read only its scale and its chords.
 */
internal fun formIn(
    scale: Scale,
    vararg chords: String,
): Form = Form(
    category = Category.JOYFUL,
    scale = scale,
    progression = Progression.parse("I"),
    harmonicRhythm = HarmonicRhythm.ONE_CHORD_PER_BAR,
    tempo = 120,
    motif = RhythmCell.parse("4"),
    bars = chords.map { Bar(Section.A, BarRole.MOTIF, listOf(chordOf(it)), RhythmCell.parse("4")) },
)

/** The major chord named by its root, such as `"C"` or `"F#"`, in octave 4, or the minor one if `m` follows it. */
internal fun chordOf(name: String): Chord = Chord(
    Pitch.parse(name.removeSuffix("m") + "4"),
    if (name.endsWith("m")) ChordQuality.MINOR else ChordQuality.MAJOR,
)

/** C major, the scale of the forms written by hand. */
internal val C_MAJOR: Scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR)
