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

    @ParameterizedTest(name = "{0}: {1} in notes of {2} beats")
    @CsvSource(
        "JOYFUL, OCTAVES, 1.0",
        "CALM, REPEATED_ROOT, 2.0",
        "EPIC, REPEATED_ROOT, 0.5",
        "MELANCHOLIC, ROOT_AND_FIFTH, 1.0",
        "MYSTERIOUS, REPEATED_ROOT, 4.0",
        "RETRO, OCTAVES, 0.5",
        "DANCE, ROOT_AND_FIFTH, 0.5",
    )
    internal fun `each category has the bass of its character, a motif of the specification at a note value of its own`(
        category: Category,
        motif: BassMotif,
        beats: Double,
    ) {
        // When
        val style = category.bass

        // Then
        assertThat(style).isEqualTo(BassStyle(motif, beats))
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

    @ParameterizedTest(name = "{0} leans to {1}")
    @CsvSource("EPIC, fifths", "MELANCHOLIC, descents", "RETRO, arpeggios")
    fun `a single category leans its melody each way, epic to fifths, melancholic down, retro to arpeggios`(
        leaning: Category,
        trait: String,
    ) {
        // Given
        val factor = mapOf<String, (MelodyTraits) -> Double>(
            "fifths" to { it.fifths },
            "descents" to { it.descents },
            "arpeggios" to { it.arpeggios },
        ).getValue(trait)

        // When
        val leanings = Category.entries.filter { factor(it.melody) != factor(MelodyTraits.NEUTRAL) }

        // Then
        assertThat(leanings).containsExactly(leaning)
        assertThat(factor(leaning.melody)).isGreaterThan(factor(MelodyTraits.NEUTRAL))
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

    @ParameterizedTest(name = "{0}: {1} of steps, {2} points")
    @CsvSource(
        "JOYFUL, 0.69, 85.5",
        "CALM, 0.69, 84.5",
        "EPIC, 0.69, 80.5",
        "MELANCHOLIC, 0.63, 87.5",
        "MYSTERIOUS, 0.59, 88.5",
        "RETRO, 0.63, 78.0",
        "DANCE, 0.61, 83.5",
    )
    fun `each category aims at the share of steps of its melodies, and stops at the 15th percentile of its best ones`(
        category: Category,
        ideal: Double,
        threshold: Double,
    ) {
        // When
        val profile = category.quality

        // Then the share of steps its melodies make with their traits, and about the 15th percentile of the best of a
        // first round, both measured over the seeds
        assertThat(profile.idealStepShare).isEqualTo(ideal)
        assertThat(profile.threshold).isEqualTo(threshold)
    }

    @Test
    fun `the categories whose melody leans aim at a share of those moves, about the 90th percentile of candidates`() {
        // When
        val leanings = Category.entries.associateWith { it.quality.leaning }

        // Then epic by fifths, melancholic down and retro by arpeggios, as their traits lean, and the others no way
        assertThat(leanings).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                Category.JOYFUL to null,
                Category.CALM to null,
                Category.EPIC to Leaning(Lean.FIFTHS, 0.10),
                Category.MELANCHOLIC to Leaning(Lean.DESCENTS, 0.70),
                Category.MYSTERIOUS to null,
                Category.RETRO to Leaning(Lean.ARPEGGIOS, 0.33),
                Category.DANCE to null,
            ),
        )
        assertThat(Category.entries.filter { it.quality.leaning != null })
            .containsExactlyInAnyOrderElementsOf(Category.entries.filter { it.melody != MelodyTraits.NEUTRAL })
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
