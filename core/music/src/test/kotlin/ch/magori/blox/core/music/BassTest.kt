package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import kotlin.math.abs
import kotlin.random.Random

/**
 * The rules of the bass, checked over many seeds and every category. The intervals, ranges and counts below are written
 * out again rather than read from the constants of the generator: a value changed on one side then fails a test instead
 * of passing unseen.
 */
class BassTest {
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a bass strikes a note at each value of its style, starting anew on each change of chord, without a rest`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, _, bass) ->
            val beats = form.category.bass.beats
            form.bars.zip(bass.bars).forEach { (bar, notes) ->
                val span = GENERATED_BEATS_PER_BAR.toDouble() / bar.chords.size
                val onsets = (0 until bar.chords.size).flatMap { chord ->
                    generateSequence(chord * span) { it + beats }.takeWhile { it < (chord + 1) * span }.toList()
                }
                val durations = (onsets + GENERATED_BEATS_PER_BAR.toDouble()).zipWithNext { start, end -> end - start }
                assertThat(notes.map { it.beats }).describedAs("%s", form.category).isEqualTo(durations)
                assertThat(notes).noneMatch { it.isRest }
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a bass keeps from C2 to C4, below every note of the melody sounding with it`(seed: Long) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (_, melody, bass) ->
            assertThat(spansOf(bass.bars)).allSatisfy { low ->
                assertThat(low.pitch.midi).describedAs("%s", low).isBetween(C2, C4)
                val above = spansOf(melody.bars).filter { it.bar == low.bar && it.overlaps(low) }
                assertThat(above).describedAs("%s", low).allMatch { it.pitch.midi > low.pitch.midi }
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a bass strikes the root of the chord on beat 1 of every bar, and on beat 3 when the chord changes there`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, _, bass) ->
            form.bars.forEachIndexed { index, bar ->
                val span = GENERATED_BEATS_PER_BAR.toDouble() / bar.chords.size
                bar.chords.forEachIndexed { at, chord ->
                    val opening = spansOf(bass.bars).single { it.bar == index && it.onset == at * span }
                    assertThat(opening.pitch.semitonesAboveInOctave(chord.root)).describedAs("%s", opening).isZero()
                }
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a bass never moves in parallel fifths or octaves with the melody, not even from bar 8 back to bar 1`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody, bass) ->
            assertThat(parallelsBetween(melody.struck, bass.struck)).describedAs("%s", form.category).isEmpty()
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `over the seeds, a bass fits nearly every melody, but fewer of mysterious, whose held roots leave it no choice`(
        category: Category,
    ) {
        // When
        val fitted = seeds().count { seed -> composedOf(category, seed) != null }

        // Then
        val least = if (category == Category.MYSTERIOUS) LEAST_MYSTERIOUS_FIT else LEAST_FIT
        assertThat(fitted).isBetween(least, seeds().size)
    }

    @Test
    fun `a melody that no bass fits leaves no bass, found in good time rather than searched for ever`() {
        // Given held roots going down by step, A G F E twice, under a melody doubling each of them in octaves: the
        // bass may leap up from A2 to G3 once, but then has no F above it in its range, and goes down with the melody
        val form = formOf(Category.MYSTERIOUS, "Am", "G", "F", "E", "Am", "G", "F", "E")
        val melody = melodyOf("A4 4", "G4 4", "F4 4", "E4 4", "A4 4", "G4 4", "F4 4", "E4 4")

        // When / Then
        assertTimeoutPreemptively(Duration.ofSeconds(SECONDS_TO_GIVE_UP)) {
            assertThat(Bass.compose(form, melody, Random(0))).isNull()
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "JOYFUL, C2 C3 C2 C3, F3 F2 F3 F2",
        "MELANCHOLIC, C2 G2 C2 G2, F2 C3 F2 C3",
        "CALM, C2 C2, F2 F2",
        "EPIC, C2 C2 C2 C2 C2 C2 C2 C2, F2 F2 F2 F2 F2 F2 F2 F2",
        "MYSTERIOUS, C2, F2",
    )
    fun `under a melody that never moves, a bass plays the motif of its category, each root nearest the note before`(
        category: Category,
        first: String,
        second: String,
    ) {
        // Given C then F, under a melody that never moves, so that the bass never moves in parallel with it: C2, the
        // lowest root, opens the music, and F3, the root nearest C3, the bar after it
        val form = formOf(category, "C", "F", "C", "F", "C", "F", "C", "F")
        val melody = melodyOf(*Array(Form.BARS.count()) { "E5 4" })

        // When
        val bass = checkNotNull(Bass.compose(form, melody, Random(0), passingChance = 0.0))

        // Then
        assertThat(bass.bars.take(2).map { bar -> bar.joinToString(" ") { it.pitch.toString() } })
            .containsExactly(first, second)
    }

    @Test
    fun `where its motif would move in parallel fifths with the melody, a bass takes another note of the chord`() {
        // Given root and fifth, C2 G2 C2 G2 under G4 D5 E5 D5: from G4 over C2 to D5 over G2, both voices would go up
        // by fifths, and so they would to G3; E2, the nearest note of the chord left, makes a seventh under D5, and E5
        // over C2 a third after it
        val form = formOf(Category.MELANCHOLIC, "C", "C", "F", "F", "C", "C", "F", "F")
        val melody = melodyOf("G4 1, D5 1, E5 1, D5 1", *Array(Form.BARS.count() - 1) { "E5 4" })

        // When
        val bass = checkNotNull(Bass.compose(form, melody, Random(0), passingChance = 0.0))

        // Then
        assertThat(bass.bars.first().map { it.pitch.toString() }).containsExactly("C2", "E2", "C2", "G2")
    }

    @Test
    fun `where its root on beat 1 would move in parallel octaves with the melody, a bass strikes it an octave up`() {
        // Given bouncing octaves, C2 C3 C2 C3, then G2 on beat 1 of bar 2, under C5 struck with C3 then G4 with G2:
        // both voices would go down by octaves, so the bass leaps up to G3 instead
        val form = formOf(Category.JOYFUL, "C", "G", "C", "G", "C", "G", "C", "G")
        val melody = melodyOf("E5 3, C5 1", "G4 4", *Array(Form.BARS.count() - 2) { "E5 4" })

        // When
        val bass = checkNotNull(Bass.compose(form, melody, Random(0), passingChance = 0.0))

        // Then
        assertThat(bass.bars.take(2).map { bar -> bar.map { it.pitch.toString() } })
            .containsExactly(listOf("C2", "C3", "C2", "C3"), listOf("G3", "G2", "G3", "G2"))
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `every bass note is a note of the chord in place, but a passing note on the last weak beat before a new chord`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, _, bass) ->
            val struck = bass.struck
            struck.forEachIndexed { index, note ->
                val chord = form.bars[note.bar].chordAt(note.onset)
                if (note.pitch !in chord) {
                    val next = struck[(index + 1) % struck.size]
                    val nextChord = form.bars[next.bar].chordAt(next.onset)
                    assertThat(nextChord).describedAs("%s before %s", note, next).isNotEqualTo(chord)
                    assertThat(note.onset).describedAs("%s", note).isNotIn(STRONG_BEATS)
                    assertThat(note.pitch in form.scale).describedAs("%s", note).isTrue()
                    assertThat(next.pitch.semitonesAboveInOctave(nextChord.root)).isZero()
                    assertThat(abs(next.pitch.midi - note.pitch.midi)).describedAs("%s to %s", note, next).isIn(1, 2)
                }
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class, names = ["JOYFUL", "EPIC", "MELANCHOLIC", "RETRO", "DANCE"])
    fun `over the seeds, a bass leads by step to the next root more often than a bass that never tries a passing note`(
        category: Category,
    ) {
        // When
        val passing = shareOfStepsToNextRoot(category, Bass.PASSING_CHANCE)
        val plain = shareOfStepsToNextRoot(category, passingChance = 0.0)

        // Then
        assertThat(passing).isGreaterThan(plain + LEAST_PASSING_GAIN).isLessThan(MOST_PASSING_SHARE)
    }

    @Test
    fun `a bass goes under a melody that comes down into its range, even if it leaves its motif`() {
        // Given bouncing octaves, C2 C3 C2 C3, under a melody that strikes C3 on beat 2
        val form = formOf(Category.JOYFUL, "C", "F", "C", "F", "C", "F", "C", "F")
        val melody = melodyOf("E5 1, C3 1, E5 2", *Array(Form.BARS.count() - 1) { "E5 4" })

        // When
        val bass = checkNotNull(Bass.compose(form, melody, Random(0), passingChance = 0.0))

        // Then
        assertThat(bass.bars.first().map { it.pitch.toString() }).containsExactly("C2", "C2", "C2", "C3")
    }

    @Test
    fun `a bass trying a passing note plays, on the last weak beat before a new chord, a step from its root`() {
        // Given bouncing octaves, C2 C3 C2 C3 then G2, under a melody that never moves, a passing note tried each time
        val form = formOf(Category.JOYFUL, "C", "G", "C", "G", "C", "G", "C", "G")
        val melody = melodyOf(*Array(Form.BARS.count()) { "E5 4" })

        // When
        val bass = checkNotNull(Bass.compose(form, melody, Random(0), passingChance = 1.0))

        // Then before G, no passing note: F2 would make a seventh with E5, A2 lies past a fourth from C2; before C,
        // B2, a third from G2, a fourth under E5, a step from the C3 that bar 3 opens on; from C3, A2 is near enough
        assertThat(bass.bars.take(3).map { bar -> bar.map { it.pitch.toString() } }).containsExactly(
            listOf("C2", "C3", "C2", "C3"),
            listOf("G2", "G3", "G2", "B2"),
            listOf("C3", "C4", "C3", "A2"),
        )
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `the same form, the same melody and the same draw give the same bass`(seed: Long) {
        // When
        val first = composedOf(seed)
        val second = composedOf(seed)

        // Then
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `the seed 42 on joyful gives this bass, which pins the order of the draws after the melody`() {
        // When
        val bass = checkNotNull(composedOf(Category.JOYFUL, 42)).bass

        // Then C major, I-IV-V-I with the half cadence on V in bars 4 and 8: each bar bounces an octave from the root
        // it opens on, bar 2 down from F3, and B3 then G3 lead by step to the next root
        assertThat(bass.bars.map { bar -> bar.map { it.pitch.toString() } }).containsExactly(
            listOf("C2", "C3", "C2", "C3"),
            listOf("F3", "F2", "F3", "F2"),
            listOf("G2", "G3", "G2", "G3"),
            listOf("G3", "G2", "G3", "B3"),
            listOf("C4", "C3", "C4", "G3"),
            listOf("F3", "F2", "F3", "F2"),
            listOf("G2", "G3", "G2", "G3"),
            listOf("G3", "G2", "G3", "G2"),
        )
    }

    @Test
    fun `a bass avoids the octaves the melody reaches while it holds, before both move on together`() {
        // Given calm, C2 struck on beat 1 and again on beat 3, then F2, under E4 G4 C5 then F5: C5 over C2 from beat 4,
        // then F5 over F2, would make parallel octaves though C5 and C2 are not struck together
        val form = formOf(Category.CALM, "C", "F", "C", "F", "C", "F", "C", "F")
        val melody = melodyOf("E4 2, G4 1, C5 1", *Array(Form.BARS.count() - 1) { "F5 4" })

        // When
        val bass = checkNotNull(Bass.compose(form, melody, Random(0), passingChance = 0.0))

        // Then
        assertThat(parallelsBetween(melody.struck, bass.struck)).isEmpty()
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a bass never leaps past an octave, not even from bar 8 back to bar 1`(seed: Long) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, _, bass) ->
            val pitches = bass.struck.map { it.pitch.midi }
            val leaps = (pitches + pitches.first()).zipWithNext { before, after -> abs(after - before) }
            assertThat(leaps).describedAs("%s", form.category).allMatch { it <= OCTAVE }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a note out of the chord is consonant with the melody sounding with it, and reached by a fourth at most`(
        seed: Long,
    ) {
        // When
        val composed = composedOf(seed)

        // Then
        assertThat(composed).allSatisfy { (form, melody, bass) ->
            val spans = spansOf(bass.bars)
            spans.forEachIndexed { index, low ->
                if (low.pitch !in form.bars[low.bar].chordAt(low.onset)) {
                    val previous = spans[(index + spans.size - 1) % spans.size]
                    val above = spansOf(melody.bars).filter { it.bar == low.bar && it.overlaps(low) }
                    assertThat(above.map { it.pitch.semitonesAboveInOctave(low.pitch) })
                        .describedAs("%s under %s", low, above).doesNotContainAnyElementsOf(DISSONANCES)
                    assertThat(
                        abs(low.pitch.midi - previous.pitch.midi),
                    ).describedAs("%s", low).isLessThanOrEqualTo(FOURTH)
                }
            }
        }
    }

    @Test
    fun `a dead end that shows only in the last bar is found in good time, each dead end searched once`() {
        // Given epic, eight eighth notes a bar, under a melody that comes down to C2 in bar 8, below the whole range of
        // the bass: every way through the first seven bars leads to the same dead end
        val form = formOf(Category.EPIC, "C", "F", "C", "F", "C", "F", "C", "F")
        val melody = melodyOf(*Array(Form.BARS.count() - 1) { "E5 4" }, "C2 4")

        // When / Then
        assertTimeoutPreemptively(Duration.ofSeconds(SECONDS_TO_GIVE_UP)) {
            assertThat(Bass.compose(form, melody, Random(0), searchBudget = Int.MAX_VALUE)).isNull()
        }
    }

    @Test
    fun `a search past its budget gives up, leaving no bass, even where one fits`() {
        // Given a melody that never moves, under which a bass fits
        val form = formOf(Category.EPIC, "C", "F", "C", "F", "C", "F", "C", "F")
        val melody = melodyOf(*Array(Form.BARS.count()) { "E5 4" })

        // When
        val bass = Bass.compose(form, melody, Random(0), searchBudget = 1)

        // Then
        assertThat(bass).isNull()
    }

    /**
     * The form, the melody and the bass of every category composed from [seed], each drawn after the one before, but
     * the melodies that no bass fits: how many there are is measured on its own.
     */
    private fun composedOf(seed: Long): List<Composed> = Category.entries.mapNotNull { composedOf(it, seed) }

    /**
     * The form, the melody and the bass of [category] composed from [seed], each drawn after the one before, or `null`
     * if no bass fits the melody.
     */
    private fun composedOf(
        category: Category,
        seed: Long,
        passingChance: Double = Bass.PASSING_CHANCE,
    ): Composed? {
        val random = Random(seed)
        val form = Form.compose(category, random)
        val melody = Melody.compose(form, random)
        return Bass.compose(form, melody, random, passingChance)?.let { Composed(form, melody, it) }
    }

    /**
     * A form of [category] playing [chords] in turn, one a bar, each written as its root in the fourth octave, followed
     * by `m` when minor: `"Am"`, `"G"`.
     */
    private fun formOf(
        category: Category,
        vararg chords: String,
    ): Form = Form.compose(category, Random(0)).let { form ->
        form.copy(bars = form.bars.zip(chords) { bar, chord -> bar.copy(chords = listOf(chordOf(chord))) })
    }

    private fun chordOf(name: String): Chord = Chord(
        Pitch.parse(name.removeSuffix("m") + "4"),
        if (name.endsWith("m")) ChordQuality.MINOR else ChordQuality.MAJOR,
    )

    /**
     * The share of the last weak beats before a new chord where the basses of [category], composed from the seeds and
     * trying passing notes with [passingChance], go on by step to the next root.
     */
    private fun shareOfStepsToNextRoot(
        category: Category,
        passingChance: Double,
    ): Double {
        val steps = seeds().mapNotNull { seed ->
            composedOf(category, seed, passingChance)
        }.flatMap { (form, _, bass) ->
            val struck = bass.struck
            struck.indices.mapNotNull { index ->
                val note = struck[index]
                val next = struck[(index + 1) % struck.size]
                val changes = form.bars[next.bar].chordAt(next.onset) != form.bars[note.bar].chordAt(note.onset)
                if (changes && note.onset !in STRONG_BEATS) form.scale.isStep(note.pitch, next.pitch) else null
            }
        }
        return steps.count { it }.toDouble() / steps.size
    }

    private data class Composed(val form: Form, val melody: Melody, val bass: Bass)

    /** A note of [pitch] sounding in [bar], counted from 0, from [onset] to [end] beats from the start of the bar. */
    private data class Span(val bar: Int, val onset: Double, val end: Double, val pitch: Pitch) {
        fun overlaps(other: Span): Boolean = onset < other.end && other.onset < end
    }

    /** The notes of [bars] as they sound, rests left out. */
    private fun spansOf(bars: List<List<Note>>): List<Span> = bars.flatMapIndexed { bar, notes ->
        val onsets = notes.runningFold(0.0) { onset, note -> onset + note.beats }
        notes.zip(onsets).mapNotNull { (note, onset) -> note.pitch?.let { Span(bar, onset, onset + note.beats, it) } }
    }

    private companion object {
        /** The range of the bass, C2 to C4, as MIDI numbers. */
        const val C2 = 36
        const val C4 = 60

        /** Semitones in an octave, the widest leap of a bass. */
        const val OCTAVE = 12

        /** Semitones in a perfect fourth, the widest leap to a note out of the chord. */
        const val FOURTH = 5

        /** The seconds, sevenths and tritone, octaves left out, that a note out of the chord never makes with the melody. */
        val DISSONANCES = listOf(1, 2, 6, 10, 11)

        /** The fewest seeds out of a hundred whose melody a bass fits: 99 measured at worst, for calm. */
        const val LEAST_FIT = 98

        /** The fewest seeds out of a hundred whose mysterious melody a bass fits: 72 measured. */
        const val LEAST_MYSTERIOUS_FIT = 65

        /** The longest a search may take before giving up on a melody that no bass fits. */
        const val SECONDS_TO_GIVE_UP = 10L

        /** Beats 1 and 3, as times in beats from the start of the bar. */
        val STRONG_BEATS = listOf(0.0, 2.0)

        /**
         * How much more of the last weak beats before a new chord lead by step to the next root with passing notes: 10
         * points measured at least, for dance, whose roots nearest the note before often come by step already.
         */
        const val LEAST_PASSING_GAIN = 0.05

        /** The most of the last weak beats before a new chord that lead by step to the next root: a tendency only. */
        const val MOST_PASSING_SHARE = 0.9

        @JvmStatic
        fun seeds(): List<Long> = (0L until 100L).toList()

        /** Enough seeds for the rules that hold the same way whatever the draw. */
        @JvmStatic
        fun fewSeeds(): List<Long> = (0L until 10L).toList()
    }
}
