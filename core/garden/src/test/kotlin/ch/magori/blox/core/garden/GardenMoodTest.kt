package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.music.Chord
import ch.magori.blox.core.music.ChordQuality
import ch.magori.blox.core.music.ChordQuality.MAJOR
import ch.magori.blox.core.music.ChordQuality.MINOR
import ch.magori.blox.core.music.Pitch
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

class GardenMoodTest {
    @ParameterizedTest(name = "{0} on {1}")
    @CsvSource("JOYFUL, C4", "CALM, C3", "MYSTERIOUS, C4", "EPIC, C4")
    fun `the bottom row is the tonic, C4 for every mood but the calm one, on C3`(
        mood: GardenMood,
        tonic: String,
    ) {
        // Given: the bottom row of the grid
        val bottom = Garden.ROWS - 1

        // When
        val pitch = mood.pitchAt(bottom)

        // Then
        assertThat(pitch).isEqualTo(Pitch.parse(tonic))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("rowsOfEachMood")
    fun `each mood has 14 rows of its scale, the tonic on the bottom row`(
        mood: GardenMood,
        bottomToTop: String,
    ) {
        // Given: the rows of the grid, from the bottom one up
        val rows = Garden.ROWS - 1 downTo 0

        // When
        val pitches = rows.map(mood::pitchAt)

        // Then
        assertThat(pitches).containsExactlyElementsOf(bottomToTop.split(" ").map(Pitch::parse))
    }

    @ParameterizedTest(name = "{0} at {1} beats a minute")
    @CsvSource("JOYFUL, 132", "CALM, 84", "MYSTERIOUS, 100", "EPIC, 120")
    fun `each mood plays at its tempo, within the range of its category`(
        mood: GardenMood,
        tempo: Int,
    ) {
        // Given: the category of the generated music the mood is heard as
        val range = mood.category.tempo

        // When
        val beatsPerMinute = mood.tempo

        // Then
        assertThat(beatsPerMinute).isEqualTo(tempo).isBetween(range.slowest, range.fastest)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("chordsOfEachMood")
    fun `each mood plays 4 chords, one per group of 4 eighths`(
        mood: GardenMood,
        numerals: String,
        chords: List<Chord>,
    ) {
        // Given: the columns of the grid, one per eighth, from left to right
        val columns = 0 until Garden.COLUMNS

        // When
        val chordOfEachColumn = columns.map(mood::chordAt)

        // Then
        assertThat(mood.progression.numerals.joinToString("–") { it.name }).isEqualTo(numerals)
        val groups = chords.flatMap { chord -> List(GardenMood.EIGHTHS_PER_CHORD) { chord } }
        assertThat(chordOfEachColumn).containsExactlyElementsOf(groups)
    }

    @ParameterizedTest(name = "{0}, column {1}, row {2}: {3}")
    @CsvSource(
        // Joyful, C major under the first group: C4 and C5 are, D4 is not
        "JOYFUL, 0, 13, true",
        "JOYFUL, 3, 6, true",
        "JOYFUL, 0, 12, false",
        // Joyful, G major under the second group: G4 is, C4 is not
        "JOYFUL, 4, 9, true",
        "JOYFUL, 7, 13, false",
        // Calm, D3 in the fourth group (G major) only
        "CALM, 12, 12, true",
        "CALM, 0, 12, false",
        // Mysterious, D4 in the second group, B flat major, whose root is out of the scale
        "MYSTERIOUS, 5, 12, true",
        // Mysterious, B4 in the second group: not in B flat major, though in the scale
        "MYSTERIOUS, 5, 7, false",
        // Epic, E flat 4 in the first group (C minor) and in the third one (E flat major), not in the last one
        "EPIC, 2, 11, true",
        "EPIC, 8, 11, true",
        "EPIC, 15, 11, false",
    )
    fun `a cell is a note of the chord when its note belongs to the chord of its group`(
        mood: GardenMood,
        column: Int,
        row: Int,
        chordNote: Boolean,
    ) {
        // Given
        val cell = Position(column, row)

        // When
        val isChordNote = mood.isChordNote(cell)

        // Then
        assertThat(isChordNote).isEqualTo(chordNote)
    }

    @ParameterizedTest(name = "column {0} in group {1}")
    @CsvSource("0, 0", "3, 0", "4, 1", "7, 1", "8, 2", "12, 3", "15, 3")
    fun `a column belongs to the group of its 4 eighths`(
        column: Int,
        group: Int,
    ) {
        assertThat(GardenMood.groupOf(column)).isEqualTo(group)
    }

    @ParameterizedTest(name = "column {0}, row {1}")
    @CsvSource("-1, 5", "16, 5", "5, -1", "5, 14")
    fun `a cell off the grid has neither note nor chord`(
        column: Int,
        row: Int,
    ) {
        // Given: a cell one step off the 16 × 14 grid
        val cell = Position(column, row)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { GardenMood.JOYFUL.isChordNote(cell) }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(GardenMood::class)
    fun `each mood plays a progression of its category`(mood: GardenMood) {
        // Given: the progressions of the category on the scale of the mood
        val progressions = mood.category.harmonies
            .filter { it.scaleType == mood.scale.type }
            .flatMap { it.progressions }

        // When / Then
        assertThat(progressions).contains(mood.progression)
    }

    private companion object {
        @JvmStatic
        fun rowsOfEachMood(): List<Arguments> = listOf(
            argumentSet(
                "joyful: C major, two octaves from C4",
                GardenMood.JOYFUL,
                "C4 D4 E4 F4 G4 A4 B4 C5 D5 E5 F5 G5 A5 B5",
            ),
            argumentSet(
                "calm: C major pentatonic, from C3 up to G5",
                GardenMood.CALM,
                "C3 D3 E3 G3 A3 C4 D4 E4 G4 A4 C5 D5 E5 G5",
            ),
            argumentSet(
                "mysterious: C harmonic minor, two octaves from C4",
                GardenMood.MYSTERIOUS,
                "C4 D4 D#4 F4 G4 G#4 B4 C5 D5 D#5 F5 G5 G#5 B5",
            ),
            argumentSet(
                "epic: C natural minor, two octaves from C4",
                GardenMood.EPIC,
                "C4 D4 D#4 F4 G4 G#4 A#4 C5 D5 D#5 F5 G5 G#5 A#5",
            ),
        )

        @JvmStatic
        fun chordsOfEachMood(): List<Arguments> = listOf(
            argumentSet(
                "joyful: C, G, Am, F",
                GardenMood.JOYFUL,
                "I–V–vi–IV",
                listOf(chord("C4", MAJOR), chord("G4", MAJOR), chord("A4", MINOR), chord("F4", MAJOR)),
            ),
            argumentSet(
                "calm: C, Am, F, G, an octave lower",
                GardenMood.CALM,
                "I–vi–IV–V",
                listOf(chord("C3", MAJOR), chord("A3", MINOR), chord("F3", MAJOR), chord("G3", MAJOR)),
            ),
            argumentSet(
                "mysterious: Cm, Bb, Ab, G",
                GardenMood.MYSTERIOUS,
                "i–♭VII–VI–V",
                listOf(chord("C4", MINOR), chord("A#4", MAJOR), chord("G#4", MAJOR), chord("G4", MAJOR)),
            ),
            argumentSet(
                "epic: Cm, Ab, Eb, Bb",
                GardenMood.EPIC,
                "i–VI–III–VII",
                listOf(chord("C4", MINOR), chord("G#4", MAJOR), chord("D#4", MAJOR), chord("A#4", MAJOR)),
            ),
        )

        fun chord(
            root: String,
            quality: ChordQuality,
        ): Chord = Chord(Pitch.parse(root), quality)
    }
}
