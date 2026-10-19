package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

class FormTest {
    @Test
    fun `a form spans eight bars in sections A A' B A'', one letter for each pair of bars`() {
        // When
        val form = Form.compose(Category.JOYFUL, Random(0))

        // Then
        assertThat(form.bars.map { it.section }).containsExactly(
            Section.A,
            Section.A,
            Section.A_PRIME,
            Section.A_PRIME,
            Section.B,
            Section.B,
            Section.A_DOUBLE_PRIME,
            Section.A_DOUBLE_PRIME,
        )
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a form plays in one of the keys, on one of the harmonies and at a tempo of its category`(seed: Long) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            val category = form.category
            assertThat(form.scale.tonic).isIn(Category.TONICS)
            assertThat(category.harmonies).anySatisfy { harmony ->
                assertThat(harmony.scaleType).isEqualTo(form.scale.type)
                assertThat(harmony.progressions).contains(form.progression)
                assertThat(harmony.harmonicRhythm).isEqualTo(form.harmonicRhythm)
            }
            assertThat(form.tempo).isBetween(category.tempo.slowest, category.tempo.fastest)
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `outside the cadences, each bar plays the next chord of the progression, one chord a bar`(seed: Long) {
        // Given
        val forms = formsOf(seed).filter { it.harmonicRhythm == HarmonicRhythm.ONE_CHORD_PER_BAR }

        // When / Then
        assertThat(forms).isNotEmpty().allSatisfy { form ->
            barsOutsideCadences.forEach { bar ->
                assertThat(form.bars[bar].chords).containsExactly(form.progression.chordAt(bar, form.scale))
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `retro moves on to the next chord of the progression every half bar, beats 1 and 2 then 3 and 4`(seed: Long) {
        // Given
        val form = Form.compose(Category.RETRO, Random(seed))

        // When / Then
        barsOutsideCadences.forEach { bar ->
            assertThat(form.bars[bar].chords).containsExactly(
                form.progression.chordAt(2 * bar, form.scale),
                form.progression.chordAt(2 * bar + 1, form.scale),
            )
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `bars 4 and 8 end on the chord of the fifth degree of the scale, a half cadence that calls back bar 1`(
        seed: Long,
    ) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            val fifth = form.scale.chord(RomanNumeral(5, fifthQualities.getValue(form.scale.type)))
            cadences.forEach { bar ->
                val chords = form.bars[bar].chords
                val chordsPerBar = form.harmonicRhythm.chordsPerBar
                assertThat(chords.last()).isEqualTo(fifth)
                assertThat(chords.dropLast(1)).containsExactlyElementsOf(
                    (0 until chordsPerBar - 1).map { form.progression.chordAt(bar * chordsPerBar + it, form.scale) },
                )
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `bar 1 of every form plays the tonic chord of its scale, which the half cadence of bar 8 calls back`(
        seed: Long,
    ) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            val tonic = Chord(form.scale.tonic, tonicQualities.getValue(form.scale.type))
            assertThat(form.bars.first().chords.first()).isEqualTo(tonic)
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `bars 1, 2, 3, 6 and 7 take up the motif, bar 5 departs from it, bars 4 and 8 end the phrases`(seed: Long) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            assertThat(form.bars.map { it.role }).containsExactly(
                BarRole.MOTIF,
                BarRole.MOTIF,
                BarRole.MOTIF,
                BarRole.PHRASE_END,
                BarRole.CONTRAST,
                BarRole.MOTIF,
                BarRole.MOTIF,
                BarRole.PHRASE_END,
            )
            assertThat(form.motif).isIn(form.category.rhythms)
            assertThat(form.bars.filter { it.role == BarRole.MOTIF }).allSatisfy {
                assertThat(it.rhythm).isEqualTo(form.motif)
            }
            assertThat(form.bars[CONTRAST_BAR].rhythm).isIn(form.category.rhythms - form.motif)
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `the phrase ends depart from the motif, so that the motif fills five bars out of eight`(seed: Long) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            assertThat(cadences.map { form.bars[it].rhythm }).doesNotContain(form.motif)
            assertThat(form.bars.count { it.rhythm == form.motif }).isEqualTo(MOTIF_BARS)
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `bar 5 departs from the phrase end of bar 4 too, so that B never takes up the end of the question`(seed: Long) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            assertThat(form.bars[CONTRAST_BAR].rhythm).isNotEqualTo(form.bars[CONTRAST_BAR - 1].rhythm)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `over the seeds, bar 5 draws every cell but the motif and the phrase end before it`(category: Category) {
        // Given
        val forms = seeds().map { seed -> Form.compose(category, Random(seed)) }

        // When
        val drawn = forms.groupBy({ it.motif }) { it.bars[CONTRAST_BAR].rhythm }

        // Then
        assertThat(drawn).allSatisfy { motif, contrasts ->
            val possible = (category.phraseEnds - motif).flatMap { end -> category.rhythms - setOf(motif, end) }
            assertThat(contrasts.toSet()).containsExactlyInAnyOrderElementsOf(possible.toSet())
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `over the seeds, bars 4 and 8 draw every phrase end but the motif`(category: Category) {
        // Given
        val forms = seeds().map { seed -> Form.compose(category, Random(seed)) }

        // When
        val drawn = forms.groupBy({ it.motif }) { form -> cadences.map { form.bars[it].rhythm } }

        // Then
        assertThat(drawn).allSatisfy { motif, ends ->
            cadences.indices.forEach { phrase ->
                assertThat(ends.map { it[phrase] }.toSet())
                    .containsExactlyInAnyOrderElementsOf(category.phraseEnds - motif)
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `each phrase breathes at its end, bars 4 and 8 closing on a long note or a silence`(seed: Long) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            cadences.forEach { bar ->
                assertThat(form.bars[bar].rhythm.breathes).isTrue()
                assertThat(form.bars[bar].rhythm).isIn(form.category.phraseEnds)
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `no bar copies the one before it, rhythm and chords alike, not even bar 1 after bar 8 in the loop`(
        seed: Long,
    ) {
        // When
        val forms = formsOf(seed)

        // Then
        assertThat(forms).allSatisfy { form ->
            val pairs = form.bars.zipWithNext() + (form.bars.last() to form.bars.first())
            assertThat(pairs).allSatisfy { (before, bar) ->
                assertThat(bar.rhythm to bar.chords).isNotEqualTo(before.rhythm to before.chords)
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `the same category and the same seed give the same form, surprise included`(seed: Long) {
        // Given
        val choices = Category.entries + Surprise

        // When
        val first = choices.map { Form.compose(it, Random(seed)) }
        val second = choices.map { Form.compose(it, Random(seed)) }

        // Then
        assertThat(second).isEqualTo(first)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `surprise draws its category first, from the same draw as the rest of the form`(seed: Long) {
        // Given
        val category = Surprise.resolve(Random(seed))

        // When
        val form = Form.compose(Surprise, Random(seed))

        // Then
        assertThat(form.category).isEqualTo(category)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `different seeds end up giving different forms, in their key, tempo, motif and bars`(category: Category) {
        // When
        val forms = seeds().map { seed -> Form.compose(category, Random(seed)) }

        // Then
        assertThat(forms.map { it.scale.tonic }.toSet()).containsExactlyInAnyOrderElementsOf(Category.TONICS)
        assertThat(forms.map { it.tempo }.toSet()).hasSizeGreaterThan(1)
        assertThat(forms.map { it.motif }.toSet()).containsExactlyInAnyOrderElementsOf(category.rhythms)
        assertThat(forms.map { it.bars }.toSet()).hasSizeGreaterThan(forms.size / 2)
    }

    @Test
    fun `the seed 42 on joyful gives this form, which pins the order of the draws`() {
        // Given
        val motif = RhythmCell.parse("0.75 0.25 0.75 0.25 1 1")
        val (c, f, g) = listOf("C4", "F4", "G4").map { Chord(Pitch.parse(it), ChordQuality.MAJOR) }

        // When
        val form = Form.compose(Category.JOYFUL, Random(42))

        // Then
        assertThat(form.scale).isEqualTo(Scale(Pitch.parse("C4"), ScaleType.MAJOR))
        assertThat(form.progression).isEqualTo(Progression.parse("I–IV–V–I"))
        assertThat(form.tempo).isEqualTo(133)
        assertThat(form.motif).isEqualTo(motif)
        assertThat(form.bars.map { it.chords.single() }).containsExactly(c, f, g, g, c, f, g, g)
        assertThat(form.bars.map { it.rhythm }).containsExactly(
            motif,
            motif,
            motif,
            RhythmCell.parse("0.5 0.5 1 2"),
            RhythmCell.parse("1 0.5 0.5 0.75 0.25 1"),
            motif,
            motif,
            RhythmCell.parse("0.75 0.25 1 2"),
        )
    }

    /** The forms of every category composed from [seed]. */
    private fun formsOf(seed: Long): List<Form> = Category.entries.map { Form.compose(it, Random(seed)) }

    private companion object {
        const val BARS = 8

        /** Bar 5, counted from 0, where section B departs from the motif. */
        const val CONTRAST_BAR = 4

        /** The bars that take up the motif: 1, 2, 3, 6 and 7. */
        const val MOTIF_BARS = 5

        /** Bars 4 and 8, counted from 0: they end each phrase on a half cadence. */
        val cadences = listOf(3, 7)
        val barsOutsideCadences = (0 until BARS) - cadences

        /**
         * The chord on the fifth degree of each scale: the major V of the major and harmonic minor scales, the minor v
         * of the modal ones, and the V of the major scale for the pentatonic one.
         */
        val fifthQualities = mapOf(
            ScaleType.MAJOR to ChordQuality.MAJOR,
            ScaleType.HARMONIC_MINOR to ChordQuality.MAJOR,
            ScaleType.MAJOR_PENTATONIC to ChordQuality.MAJOR,
            ScaleType.NATURAL_MINOR to ChordQuality.MINOR,
            ScaleType.DORIAN to ChordQuality.MINOR,
            ScaleType.MIXOLYDIAN to ChordQuality.MINOR,
        )

        /** The chord on the tonic of each scale: major where the third degree is major, minor where it is minor. */
        val tonicQualities = mapOf(
            ScaleType.MAJOR to ChordQuality.MAJOR,
            ScaleType.MIXOLYDIAN to ChordQuality.MAJOR,
            ScaleType.MAJOR_PENTATONIC to ChordQuality.MAJOR,
            ScaleType.NATURAL_MINOR to ChordQuality.MINOR,
            ScaleType.HARMONIC_MINOR to ChordQuality.MINOR,
            ScaleType.DORIAN to ChordQuality.MINOR,
        )

        @JvmStatic
        fun seeds(): List<Long> = (0L until 100L).toList()

        /** Enough seeds for the rules that hold the same way whatever the draw. */
        @JvmStatic
        fun fewSeeds(): List<Long> = (0L until 10L).toList()
    }
}
