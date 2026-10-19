package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

/**
 * The rules of the melody, checked over many seeds and every category. The intervals, bars and counts written below
 * are oracles of their own rather than the internal constants of the generator: a change of value on one side shows as
 * a failing test instead of passing unseen.
 */
class MelodyTest {
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a melody fills the rhythm of each bar of its form, a note for each sound and a rest for each silence`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody) ->
            assertThat(melody.bars.map { bar -> bar.map { RhythmStep(it.beats, it.isRest) } })
                .containsExactlyElementsOf(form.bars.map { it.rhythm.steps })
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `every note struck on beat 1 or 3 is a note of the chord in place, of its half bar when two chords share it`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody) ->
            val onStrongBeats = melody.struck.filter { it.onset in STRONG_BEATS }
            assertThat(onStrongBeats).isNotEmpty().allSatisfy { struck ->
                val chords = form.bars[struck.bar].chords
                val chord = chords[(struck.onset * chords.size / GENERATED_BEATS_PER_BAR).toInt()]
                assertThat(struck.pitch in chord).describedAs("%s under %s", struck, chord).isTrue()
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `every note is in the scale, but a chromatic passing note of mysterious, on a weak beat between two steps`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody) ->
            val struck = melody.struck
            struck.forEachIndexed { index, note ->
                if (note.pitch !in form.scale) {
                    val (before, after) = struck[index - 1].pitch.midi to struck[index + 1].pitch.midi
                    assertThat(form.category.allowsChromaticPassingNotes).describedAs("%s", note).isTrue()
                    assertThat(note.onset).isNotIn(STRONG_BEATS)
                    assertThat(listOf(note.pitch.midi - before, after - note.pitch.midi))
                        .describedAs("%s passing from %s to %s", note, before, after)
                        .isIn(listOf(1, 1), listOf(-1, -1))
                }
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a melody never strikes the same note more than three times in a row, rests left out, loop included`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (_, melody) ->
            val repeated = loopOf(melody).windowed(MOST_REPEATED_NOTES + 1).filter { notes ->
                notes.distinct().size == 1
            }
            assertThat(repeated).isEmpty()
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a melody spans a tenth at most, within the range of the melody voice, from C4 to C6`(seed: Long) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(Melody.RANGE).isEqualTo(PitchRange(Pitch.parse("C4"), Pitch.parse("C6")))
        assertThat(composed).allSatisfy { (_, melody) ->
            val pitches = melody.struck.map { it.pitch }
            assertThat(pitches).allSatisfy { assertThat(it in Melody.RANGE).describedAs("%s", it).isTrue() }
            assertThat(pitches.maxOf { it.midi } - pitches.minOf { it.midi }).isLessThanOrEqualTo(MAJOR_TENTH)
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `a melody keeps within a narrower range given to it, its window cut to that range`(seed: Long) {
        // Given an octave from G4 to G5, narrower than the major tenth of the window
        val range = PitchRange(Pitch.parse("G4"), Pitch.parse("G5"))

        // When
        val melodies = Category.entries.map { category ->
            val random = Random(seed)
            Melody.compose(Form.compose(category, random), random, range = range)
        }

        // Then
        assertThat(melodies.flatMap { it.struck }).allSatisfy {
            assertThat(it.pitch in range).describedAs("%s", it).isTrue()
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a melody never leaps past an octave, nor by a tritone, not even from bar 8 back to bar 1`(seed: Long) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (_, melody) ->
            val leaps = intervalsOf(melody).map { abs(it) }
            assertThat(leaps).allSatisfy { assertThat(it).isLessThanOrEqualTo(OCTAVE).isNotEqualTo(TRITONE) }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `after a leap of a fourth or more, the melody turns back by step, across the loop too`(seed: Long) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody) ->
            val turns = loopOf(melody).windowed(3).filter { (first, leaped) ->
                abs(leaped.midi - first.midi) >= FOURTH
            }
            assertThat(turns).allSatisfy { (first, leaped, next) ->
                assertThat((next.midi - leaped.midi).sign).isEqualTo(-(leaped.midi - first.midi).sign)
                assertThat(form.scale.isStep(leaped, next)).describedAs("%s to %s", leaped, next).isTrue()
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `the last note struck in bar 8 is a note of the half cadence chord, which calls back bar 1`(seed: Long) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody) ->
            val last = melody.struck.last()
            val cadence = form.bars.last().chords.last()
            assertThat(last.bar).isEqualTo(form.bars.lastIndex)
            assertThat(last.pitch in cadence).describedAs("%s under %s", last, cadence).isTrue()
        }
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(
        "JOYFUL, 0.75",
        "CALM, 0.35",
        "EPIC, 0.75",
        "MELANCHOLIC, 0.65",
        "MYSTERIOUS, 0.55",
        "RETRO, 0.8",
        "DANCE, 0.75",
    )
    fun `over the seeds, the bars taking up the motif keep most of its moves, in degrees of the scale, calm the fewest`(
        category: Category,
        least: Double,
    ) {
        // Given, calm keeping about 40 % of the moves, as its IV and V chords hold two notes of its pentatonic scale only
        val composed = seeds().map { seed -> composedOf(category, seed) }

        // When
        val kept = composed.flatMap { (form, melody) ->
            val contours = melody.bars.map { contourOf(it, form.scale) }
            MOTIF_REPEATS.flatMap { bar -> contours[bar].zip(contours.first()) { move, motif -> move == motif } }
        }

        // Then
        assertThat(kept.count { it }.toDouble() / kept.size).isGreaterThanOrEqualTo(least)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `over the seeds, a melody goes mostly by step, about 70 percent of its moves`(category: Category) {
        // Given
        val composed = seeds().map { seed -> composedOf(category, seed) }

        // When
        val share = composed.map { (form, melody) -> melody.stepShare(form.scale) }.average()

        // Then
        assertThat(share).isBetween(LEAST_STEP_SHARE, MOST_STEP_SHARE)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `over the seeds, a melody almost always arches up to its peak, struck once on a strong beat of bar 5 or 6`(
        category: Category,
    ) {
        // Given
        val composed = seeds().map { seed -> composedOf(category, seed) }

        // When
        val arched = composed.count { (_, melody) ->
            val highest = melody.struck.maxOf { it.pitch.midi }
            val peak = melody.struck.filter { it.pitch.midi == highest }.singleOrNull()
            peak != null && peak.bar in PEAK_BARS && peak.onset in STRONG_BEATS
        }

        // Then
        assertThat(arched.toDouble() / composed.size).isGreaterThanOrEqualTo(LEAST_ARCHED_SHARE)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `over the seeds, bars 5 and 6 mostly sing higher than bars 1 and 8, the foot of the arch`(category: Category) {
        // Given
        val composed = seeds().map { seed -> composedOf(category, seed) }

        // When
        val higher = composed.count { (_, melody) ->
            val height = { bars: List<Int> -> melody.struck.filter { it.bar in bars }.map { it.pitch.midi }.average() }
            height(PEAK_BARS) > height(ARCH_FEET)
        }

        // Then
        assertThat(higher.toDouble() / composed.size).isGreaterThanOrEqualTo(LEAST_ARCHED_SHARE_OF_HEIGHTS)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `a bar taking up the motif on the chord of the bar before still departs from it`(seed: Long) {
        // Given bar 2 on the chord of bar 1, where the motif taken up as it is would copy bar 1
        val form = joyfulFormInC().let { form -> form.withChords(1, form.bars.first().chords.single()) }

        // When
        val melody = Melody.compose(form, Random(seed))

        // Then
        assertThat(melody.bars[1]).isNotEqualTo(melody.bars[0])
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `no bar of a melody copies the one before it, pitches and rhythm alike, not even bar 1 after bar 8`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (_, melody) ->
            val pairs = melody.bars.zipWithNext() + (melody.bars.last() to melody.bars.first())
            assertThat(pairs).allSatisfy { (before, bar) -> assertThat(bar).isNotEqualTo(before) }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `a search for an arch that gives up leaves a melody without one, rather than no melody at all`(
        category: Category,
    ) {
        // When
        val melodies = seeds().map { seed -> composedOf(category, seed, archedSearchBudget = 0).second }

        // Then
        assertThat(melodies).anySatisfy { assertThat(it.peakBars.singleOrNull()).isNotIn(PEAK_BARS) }
    }

    @Test
    fun `over the seeds, epic leaps by a fifth more often than a melody leaning no way on the same forms`() {
        // When
        val epic = averageOf(Category.EPIC) { _, melody -> melody.fifthShare }
        val neutral = averageOf(Category.EPIC, MelodyTraits.NEUTRAL) { _, melody -> melody.fifthShare }

        // Then
        assertThat(epic).isGreaterThan(neutral * LEAST_TRAIT_LIFT)
    }

    @Test
    fun `over the seeds, melancholic goes down more often than a melody leaning no way on the same forms`() {
        // When
        val melancholic = averageOf(Category.MELANCHOLIC) { _, melody -> melody.descentShare }
        val neutral = averageOf(Category.MELANCHOLIC, MelodyTraits.NEUTRAL) { _, melody -> melody.descentShare }

        // Then
        assertThat(melancholic).isGreaterThan(neutral + LEAST_DESCENT_GAIN)
    }

    @Test
    fun `over the seeds, retro runs arpeggios more often than a melody leaning no way on the same forms`() {
        // When
        val retro = averageOf(Category.RETRO) { form, melody -> melody.arpeggioShare(form.bars) }
        val neutral =
            averageOf(Category.RETRO, MelodyTraits.NEUTRAL) { form, melody -> melody.arpeggioShare(form.bars) }

        // Then
        assertThat(retro).isGreaterThan(neutral * LEAST_TRAIT_LIFT)
    }

    @Test
    fun `over the seeds, mysterious passes now and then through a chromatic note, but not in most melodies`() {
        // When
        val chromatic = seeds().map { composedOf(Category.MYSTERIOUS, it) }.count { (form, melody) ->
            melody.struck.any { it.pitch !in form.scale }
        }

        // Then
        assertThat(chromatic.toDouble() / seeds().size).isBetween(LEAST_CHROMATIC_SHARE, MOST_CHROMATIC_SHARE)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `the same form and the same draw give the same melody`(seed: Long) {
        // When
        val first = composedOf(seed)
        val second = composedOf(seed)

        // Then
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `the seed 42 on joyful gives this melody, which pins the order of the draws after the form`() {
        // When
        val (_, melody) = composedOf(Category.JOYFUL, 42)

        // Then
        assertThat(pitchNamesOf(melody)).containsExactly(
            listOf("E4", "G4", "A4", "C5", "G4", "A4"),
            listOf("A4", "C5", "D5", "F5", "C5", "D5"),
            listOf("B4", "D5", "E5", "F5", "D5", "E5"),
            listOf("D5", "C5", "B4", "D5"),
            listOf("C5", "D5", "F5", "G5", "F5", "E5"),
            listOf("C5", "E5", "F5", "F5", "C5", "D5"),
            listOf("B4", "D5", "E5", "F5", "D5", "E5"),
            listOf("B4", "C5", "A4", "G4"),
        )
    }

    @Test
    fun `the seed 1 on mysterious gives this melody in D dorian, which passes through D sharp and C sharp`() {
        // When
        val (form, melody) = composedOf(Category.MYSTERIOUS, 1)

        // Then
        assertThat(form.scale).isEqualTo(Scale(Pitch.parse("D4"), ScaleType.DORIAN))
        assertThat(pitchNamesOf(melody)).containsExactly(
            listOf("F4", "A4", "G4"),
            listOf("B4", "D5", "C5"),
            listOf("D5", "E5", "D5"),
            listOf("E5"),
            listOf("D5", "F5"),
            listOf("D5", "E5", "D#5"),
            listOf("D5", "C#5", "C5"),
            listOf("A4", "A4"),
        )
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `each phrase ends on a note of the chord in place, even when its last note falls on a weak beat`(seed: Long) {
        // Given phrase ends whose last note is struck on beat 4
        val end = RhythmCell.parse("1 1 1 0.5 r0.5")
        val form = joyfulFormInC().let { form ->
            form.copy(bars = form.bars.map { if (it.role == BarRole.PHRASE_END) it.copy(rhythm = end) else it })
        }

        // When
        val melody = Melody.compose(form, Random(seed))

        // Then
        assertThat(PHRASE_ENDS).allSatisfy { bar ->
            val last = melody.struck.last { it.bar == bar }
            assertThat(last.pitch in form.bars[bar].chords.last()).describedAs("%s", last).isTrue()
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `under a borrowed chord, no note stands a semitone from a note of the chord out of the scale, a false relation`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody) ->
            val harmony = Scale(form.scale.tonic, form.scale.type.harmony)
            val clashes = melody.struck.filter { struck ->
                val chord = form.bars[struck.bar].chordAt(struck.onset)
                chord.pitches.filterNot { it in harmony }.any { borrowed ->
                    struck.pitch.semitonesAboveInOctave(borrowed) in listOf(1, OCTAVE - 1)
                }
            }
            assertThat(clashes).isEmpty()
        }
    }

    @ParameterizedTest(name = "bar {0}")
    @ValueSource(ints = [7, 0])
    fun `a form that no melody fits is refused in good time, rather than searched for ever`(bar: Int) {
        // Given C major, with an F sharp major chord, none of whose notes is in the scale, in bar 8 then in bar 1
        val form = joyfulFormInC().withChords(bar, Chord(Pitch.parse("F#4"), ChordQuality.MAJOR))

        // When / Then
        assertTimeoutPreemptively(Duration.ofSeconds(SECONDS_TO_GIVE_UP)) {
            assertThatThrownBy { Melody.compose(form, Random(0)) }.isInstanceOf(IllegalStateException::class.java)
        }
    }

    /** The form of joyful drawn from the seed 42, in C major. */
    private fun joyfulFormInC(): Form = Form.compose(Category.JOYFUL, Random(JOYFUL_IN_C))

    /** This form with [chord] played all along [bar], counted from 0. */
    private fun Form.withChords(
        bar: Int,
        chord: Chord,
    ): Form = copy(bars = bars.mapIndexed { index, it -> if (index == bar) it.copy(chords = listOf(chord)) else it })

    /**
     * The form and the melody of every category composed from [seed], the melody drawn after the form, with or without
     * its arch: the rules hold all the same when the search for an arch gives up.
     */
    private fun composedOf(seed: Long): List<Pair<Form, Melody>> = Category.entries.flatMap { category ->
        listOf(composedOf(category, seed), composedOf(category, seed, archedSearchBudget = 0))
    }

    /** The form and the melody of [category] composed from [seed], the melody drawn after the form. */
    private fun composedOf(
        category: Category,
        seed: Long,
        traits: MelodyTraits = category.melody,
        archedSearchBudget: Int = Melody.ARCHED_SEARCH_BUDGET,
    ): Pair<Form, Melody> {
        val random = Random(seed)
        val form = Form.compose(category, random)
        return form to Melody.compose(form, random, traits, archedSearchBudget = archedSearchBudget)
    }

    /** The names of the pitches struck in each bar of [melody], rests left out. */
    private fun pitchNamesOf(melody: Melody): List<List<String>> =
        melody.bars.map { bar -> bar.mapNotNull { it.pitch?.toString() } }

    /** The average of [measure] over the melodies of [category] leaning as [traits] say, composed from the seeds. */
    private fun averageOf(
        category: Category,
        traits: MelodyTraits = category.melody,
        measure: (Form, Melody) -> Double,
    ): Double = seeds()
        .map { seed -> composedOf(category, seed, traits).let { (form, melody) -> measure(form, melody) } }
        .average()

    /** The moves from each note struck in [bar] to the next one, in degrees of [scale]: +1 a step up, 0 a repeat. */
    private fun contourOf(
        bar: List<Note>,
        scale: Scale,
    ): List<Int> = bar.mapNotNull { it.pitch }.zipWithNext { before, after ->
        val low = minOf(before.midi, after.midi)
        val high = maxOf(before.midi, after.midi)
        (low + 1..high).count { Pitch(it) in scale } * (after.midi - before.midi).sign
    }

    /**
     * The pitches struck in [melody], rests left out, followed by its first three as the music loops: the rules on moves
     * hold from bar 8 back to bar 1 too.
     */
    private fun loopOf(melody: Melody): List<Pitch> = melody.struck.map { it.pitch }.let { it + it.take(LOOPED_NOTES) }

    /** The intervals in semitones from each note struck in [melody] to the next one, rests left out, loop included. */
    private fun intervalsOf(melody: Melody): List<Int> = loopOf(melody).zipWithNext { before, after ->
        after.midi -
            before.midi
    }

    private companion object {
        /** Semitones in a perfect fourth, the smallest leap the melody turns back from. */
        const val FOURTH = 5
        const val OCTAVE = 12
        const val TRITONE = 6

        /** The most times the melody strikes the same note in a row. */
        const val MOST_REPEATED_NOTES = 3

        /** The first notes taken up again after the last one, enough to check every rule on moves across the loop. */
        const val LOOPED_NOTES = 3

        /** Bars 4 and 8, counted from 0, which end the phrases. */
        val PHRASE_ENDS = listOf(3, 7)

        /** The seed whose joyful form plays in C major. */
        const val JOYFUL_IN_C = 42L

        /** The longest a search may take before refusing a form that no melody fits. */
        const val SECONDS_TO_GIVE_UP = 5L

        /** Semitones in a major tenth, the widest span of a melody. */
        const val MAJOR_TENTH = 16

        /** Bars 2, 3, 6 and 7, counted from 0: they take up the motif stated in bar 1. */
        val MOTIF_REPEATS = listOf(1, 2, 5, 6)

        /** The range of the average share of steps around 70 %, over the seeds. */
        const val LEAST_STEP_SHARE = 0.55
        const val MOST_STEP_SHARE = 0.85

        /** Bars 5 and 6, counted from 0, two thirds into the music, where the melody reaches its highest note. */
        val PEAK_BARS = listOf(4, 5)

        /** The share of the seeds whose melody reaches its highest note once, on a strong beat of bar 5 or 6. */
        const val LEAST_ARCHED_SHARE = 0.95

        /** Bars 1 and 8, counted from 0, the foot of the arch. */
        val ARCH_FEET = listOf(0, 7)

        /** The share of the seeds whose bars 5 and 6 sing higher, on average, than bars 1 and 8. */
        const val LEAST_ARCHED_SHARE_OF_HEIGHTS = 0.9

        /** How many times more often a melody does what its category leans to than a melody leaning no way. */
        const val LEAST_TRAIT_LIFT = 1.5

        /** How much more of its moves a melody leaning down takes down than a melody leaning no way. */
        const val LEAST_DESCENT_GAIN = 0.05

        /** The range of the share of the seeds whose melody, in mysterious, passes through a chromatic note. */
        const val LEAST_CHROMATIC_SHARE = 0.2
        const val MOST_CHROMATIC_SHARE = 0.6

        /** Beats 1 and 3, as times in beats from the start of the bar. */
        val STRONG_BEATS = listOf(0.0, 2.0)

        @JvmStatic
        fun seeds(): List<Long> = (0L until 100L).toList()

        /** Enough seeds for the rules that hold the same way whatever the draw. */
        @JvmStatic
        fun fewSeeds(): List<Long> = (0L until 10L).toList()
    }
}
