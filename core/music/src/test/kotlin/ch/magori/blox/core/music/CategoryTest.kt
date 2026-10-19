package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class CategoryTest {
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(
        "JOYFUL, MAJOR",
        "CALM, MAJOR_PENTATONIC",
        "EPIC, NATURAL_MINOR",
        "MELANCHOLIC, NATURAL_MINOR",
        "MYSTERIOUS, DORIAN HARMONIC_MINOR",
        "RETRO, MAJOR MIXOLYDIAN",
        "DANCE, HARMONIC_MINOR",
    )
    fun `each category plays on the scales of its character`(
        category: Category,
        scaleTypes: String,
    ) {
        // When
        val types = category.harmonies.map { it.scaleType }

        // Then
        assertThat(types).containsExactlyElementsOf(scaleTypes.split(" ").map(ScaleType::valueOf))
    }

    @ParameterizedTest(name = "{0} on {1}: {2}")
    @CsvSource(
        delimiter = ';',
        value = [
            "JOYFUL; MAJOR; I–V–vi–IV, I–IV–V–I",
            "CALM; MAJOR_PENTATONIC; I–vi–IV–V, I–IV",
            "EPIC; NATURAL_MINOR; i–VI–III–VII, i–VII–VI–VII",
            "MELANCHOLIC; NATURAL_MINOR; i–VI–iv–v, i–iv–VII–III",
            "MYSTERIOUS; DORIAN; i–IV",
            "MYSTERIOUS; HARMONIC_MINOR; i–♭VII–VI–V",
            "RETRO; MAJOR; I–V–vi–IV",
            "RETRO; MIXOLYDIAN; I–VII–IV–I",
            "DANCE; HARMONIC_MINOR; i–iv–V–i",
        ],
    )
    fun `each scale of a category comes with the progressions that suit it`(
        category: Category,
        scaleType: ScaleType,
        progressions: String,
    ) {
        // When
        val harmony = category.harmonies.single { it.scaleType == scaleType }

        // Then
        assertThat(harmony.progressions).containsExactlyElementsOf(progressions.split(", ").map(Progression::parse))
    }

    @ParameterizedTest(name = "{0}: {1} to {2}")
    @CsvSource(
        "JOYFUL, 120, 150",
        "CALM, 70, 95",
        "EPIC, 110, 140",
        "MELANCHOLIC, 80, 100",
        "MYSTERIOUS, 90, 110",
        "RETRO, 140, 170",
        "DANCE, 130, 160",
    )
    fun `each category plays within its own range of tempos, in beats per minute`(
        category: Category,
        slowest: Int,
        fastest: Int,
    ) {
        // When
        val tempo = category.tempo

        // Then
        assertThat(tempo).isEqualTo(TempoRange(slowest, fastest))
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(
        "JOYFUL, BOUNCING_OCTAVES",
        "CALM, WHOLE_AND_HALF_NOTES",
        "EPIC, GALLOP",
        "MELANCHOLIC, ROOT_AND_FIFTH",
        "MYSTERIOUS, HELD_NOTES",
        "RETRO, FAST_OCTAVES",
        "DANCE, MARKED_ROOT_AND_FIFTH",
    )
    fun `each category has the bass of its character`(
        category: Category,
        bass: BassStyle,
    ) {
        // When
        val style = category.bass

        // Then
        assertThat(style).isEqualTo(bass)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `every cell of a category plays a chord note on beats 1 and 3, and holds one light syncopation at most`(
        category: Category,
    ) {
        // When
        val cells = category.rhythms

        // Then
        assertThat(cells).allSatisfy { cell ->
            assertThat(cell.restsOnStrongBeat).isFalse()
            assertThat(cell.syncopations).isLessThanOrEqualTo(1)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `every category has cells to end its phrases on, that breathe after striking a chord note on beat 1`(
        category: Category,
    ) {
        // When
        val cells = category.phraseEnds

        // Then
        assertThat(cells).isNotEmpty().allSatisfy { cell ->
            assertThat(cell.breathes).isTrue()
            assertThat(cell.steps.first().isRest).isFalse()
            assertThat(cell.syncopations).isLessThanOrEqualTo(1)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `whatever its motif, a category keeps a phrase end apart from it, and a contrast apart from both`(
        category: Category,
    ) {
        // When
        val motifs = category.rhythms

        // Then
        assertThat(motifs).allSatisfy { motif ->
            val ends = category.phraseEnds - motif
            assertThat(ends).isNotEmpty().allSatisfy { end ->
                assertThat(category.rhythms - setOf(motif, end)).isNotEmpty()
            }
        }
    }

    @Test
    fun `calm leaves air, in half and quarter notes and some rests`() {
        // When
        val steps = Category.CALM.rhythms.flatMap { it.steps }

        // Then
        assertThat(steps).allSatisfy { assertThat(it.beats).isGreaterThanOrEqualTo(1.0) }
        assertThat(steps).anySatisfy { assertThat(it.isRest).isTrue() }
    }

    @Test
    fun `the busiest bar of calm holds fewer notes than the sparsest bar of retro`() {
        // When
        val busiestCalm = Category.CALM.rhythms.maxOf(::notesIn)
        val sparsestRetro = Category.RETRO.rhythms.minOf(::notesIn)

        // Then
        assertThat(busiestCalm).isLessThan(sparsestRetro)
    }

    @Test
    fun `retro runs a whole beat of sixteenth notes, for its fast arpeggios`() {
        // When
        val beatsOfFourNotes = Category.RETRO.rhythms.flatMap { cell -> cell.steps.windowed(SIXTEENTHS_PER_BEAT) }

        // Then
        assertThat(beatsOfFourNotes).contains(List(SIXTEENTHS_PER_BEAT) { RhythmStep(1.0 / SIXTEENTHS_PER_BEAT) })
    }

    @Test
    fun `joyful skips along on a dotted eighth and a sixteenth`() {
        // When
        val pairs = Category.JOYFUL.rhythms.flatMap { cell -> cell.steps.zipWithNext() }

        // Then
        assertThat(pairs).contains(RhythmStep(0.75) to RhythmStep(0.25))
    }

    @ParameterizedTest(name = "{0} strikes beats {1}")
    @CsvSource("EPIC, 1 2 3 4", "DANCE, 1 3")
    fun `epic strikes a strong note on every beat, and dance marks beats 1 and 3`(
        category: Category,
        beats: String,
    ) {
        // Given
        val struck = beats.split(" ").map { it.toDouble() - 1 }

        // When
        val strikes = category.rhythms.map(::strikesOf)

        // Then
        assertThat(strikes).allSatisfy { assertThat(it).containsAll(struck) }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["MELANCHOLIC", "MYSTERIOUS"])
    fun `melancholic and mysterious hold a long note, a half note at least, in every bar`(category: Category) {
        // When
        val longest = category.rhythms.map { cell -> cell.steps.filterNot { it.isRest }.maxOf { it.beats } }

        // Then
        assertThat(longest).allSatisfy { assertThat(it).isGreaterThanOrEqualTo(2.0) }
    }

    @Test
    fun `only mysterious lets its melody pass through a note out of the scale`() {
        // When
        val chromatic = Category.entries.filter { it.allowsChromaticPassingNotes }

        // Then
        assertThat(chromatic).containsExactly(Category.MYSTERIOUS)
    }

    @Test
    fun `every category may be played in C, D, F, G or A`() {
        // When
        val tonics = Category.TONICS

        // Then
        assertThat(tonics).containsExactly(
            Pitch.parse("C4"),
            Pitch.parse("D4"),
            Pitch.parse("F4"),
            Pitch.parse("G4"),
            Pitch.parse("A4"),
        )
    }

    @Test
    fun `only retro, the fastest category, changes chord twice a bar`() {
        // When
        val twice = Category.entries.filter { category ->
            category.harmonies.any { it.harmonicRhythm == HarmonicRhythm.TWO_CHORDS_PER_BAR }
        }

        // Then
        assertThat(twice).containsExactly(Category.RETRO)
        assertThat(Category.RETRO.harmonies).allSatisfy {
            assertThat(it.harmonicRhythm).isEqualTo(HarmonicRhythm.TWO_CHORDS_PER_BAR)
        }
        assertThat(Category.RETRO.tempo.fastest).isEqualTo(Category.entries.maxOf { it.tempo.fastest })
    }

    private fun notesIn(cell: RhythmCell): Int = cell.steps.count { !it.isRest }

    /** Times in beats, from the start of the bar, at which [cell] strikes a note. */
    private fun strikesOf(cell: RhythmCell): List<Double> =
        cell.steps.runningFold(0.0) { onset, step -> onset + step.beats }
            .zip(cell.steps)
            .filterNot { (_, step) -> step.isRest }
            .map { (onset, _) -> onset }

    private companion object {
        const val SIXTEENTHS_PER_BEAT = 4
    }
}
