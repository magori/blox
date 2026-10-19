package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

/**
 * The choice of the best candidate and the music it gives. The rules that hold over the seeds look at generations
 * composed once for all of them, [GENERATIONS], as composing a music is costly.
 */
class MusicGeneratorTest {
    @Test
    fun `over the seeds and the categories, the generator keeps the best of the candidates it composes`() {
        // When
        val generations = GENERATIONS.values.flatten()

        // Then
        assertThat(generations).allSatisfy { generation ->
            val qualities = generation.rounds.flatten().map { it.quality.points }
            assertThat(generation.best.quality.points).isEqualTo(qualities.max())
        }
    }

    @Test
    fun `over the seeds, a round composes twenty candidates, each from a draw of its own derived from the seed`() {
        // When joyful, whose candidates are never ruled out
        val rounds = GENERATIONS.getValue(Category.JOYFUL).flatMap { it.rounds }

        // Then
        assertThat(rounds).allSatisfy { round ->
            assertThat(round).hasSize(20)
            assertThat(round.map { it.melody }.distinct()).hasSize(20)
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `while no candidate reaches the threshold, a new round is composed, three at most, and the best one is kept`(
        seed: Long,
    ) {
        // When the threshold is out of reach
        val generation = MusicGenerator.choose(Category.JOYFUL, seed, threshold = 101.0)

        // Then
        assertThat(generation.rounds).hasSize(3).allSatisfy { assertThat(it).hasSize(20) }
        assertThat(generation.rounds.flatten().map { it.melody }.distinct()).hasSize(60)
        assertThat(generation.best.quality.points).isEqualTo(generation.rounds.flatten().maxOf { it.quality.points })
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `a first round whose best candidate reaches the threshold exactly is the last one`(seed: Long) {
        // Given the points of the best candidate of the first round
        val first = MusicGenerator.choose(Category.JOYFUL, seed, threshold = 101.0).rounds.first()
        val threshold = first.maxOf { it.quality.points }

        // When
        val generation = MusicGenerator.choose(Category.JOYFUL, seed, threshold)

        // Then
        assertThat(generation.rounds).containsExactly(first)
    }

    @Test
    fun `among candidates of equal points, the first one composed is kept`() {
        // Given two candidates alike but for their tempo, which the note of quality does not look at
        val candidate = GENERATIONS.getValue(Category.JOYFUL).first().best
        val slower = candidate.copy(form = candidate.form.copy(tempo = candidate.form.tempo - 1))

        // When
        val forward = Generation(Category.JOYFUL, listOf(listOf(candidate, slower)))
        val backward = Generation(Category.JOYFUL, listOf(listOf(slower), listOf(candidate)))

        // Then
        assertThat(slower.quality).isEqualTo(candidate.quality)
        assertThat(forward.best).isSameAs(candidate)
        assertThat(backward.best).isSameAs(slower)
    }

    @Test
    fun `surprise gives back the category it drew from the seed, played by all the candidates, and the scale chosen`() {
        // When
        val musics = fewSeeds().associateWith { seed -> MusicGenerator.generate(Surprise, seed, AN_ID) }

        // Then
        assertThat(musics).allSatisfy { seed, music ->
            val generation = MusicGenerator.choose(Surprise, seed)
            assertThat(generation.rounds.flatten()).allSatisfy {
                assertThat(it.form.category).isEqualTo(music.category)
            }
            assertThat(music.scale).isEqualTo(generation.best.form.scale)
            assertThat(music.choice).isEqualTo(Surprise)
            assertThat(music.seed).isEqualTo(seed)
        }
        assertThat(musics.values.map { it.category }.toSet()).hasSizeGreaterThan(1)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `a generated score, known by the id it is given, plays the best melody and bass as the arrangements do`(
        category: Category,
    ) {
        // Given
        val generation = GENERATIONS.getValue(category).first()

        // When
        val music = MusicGenerator.assemble(generation, category, seed = 0, id = AN_ID)

        // Then
        val best = generation.best
        val (melody, bass) = Scores.korobeiniki.voices
        assertThat(music.score.id).isEqualTo(AN_ID)
        assertThat(music.score.bpm).isEqualTo(best.form.tempo)
        assertThat(music.score.beatsPerBar).isEqualTo(GENERATED_BEATS_PER_BAR)
        assertThat(music.score.voices).containsExactly(
            Voice(melody.waveform, melody.volume, best.melody.bars.flatten()),
            Voice(bass.waveform, bass.volume, best.bass.bars.flatten()),
        )
        assertThat(music.category).isEqualTo(category)
        assertThat(music.scale).isEqualTo(best.form.scale)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("fewSeeds")
    fun `the same choice and the same seed give the same music, surprise included, and other seeds other musics`(
        seed: Long,
    ) {
        // Given
        val choices = Category.entries + Surprise

        // When
        val first = choices.map { MusicGenerator.generate(it, seed, AN_ID) }
        val again = choices.map { MusicGenerator.generate(it, seed, AN_ID) }
        val next = choices.map { MusicGenerator.generate(it, seed + 1, AN_ID) }

        // Then
        assertThat(again).isEqualTo(first)
        assertThat(next.zip(first)).allSatisfy { (other, music) -> assertThat(other.score).isNotEqualTo(music.score) }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `over the seeds, a generated score plays two voices of eight bars of four beats, at a tempo of its category`(
        category: Category,
    ) {
        // When
        val scores = GENERATIONS.getValue(category).map {
            MusicGenerator.assemble(it, category, seed = 0, id = AN_ID).score
        }

        // Then each voice strikes a note on every bar line, none held across it
        val barLines = (0..BARS).map { it * 4.0 }
        assertThat(scores).allSatisfy { score ->
            assertThat(score.bpm).isBetween(category.tempo.slowest, category.tempo.fastest)
            assertThat(score.beatsPerBar).isEqualTo(4)
            assertThat(score.voices).hasSize(2).allSatisfy { voice ->
                assertThat(voice.notes.runningFold(0.0) { onset, note -> onset + note.beats }).containsAll(barLines)
                assertThat(voice.beats).isEqualTo(BARS * 4.0)
            }
        }
    }

    @Test
    fun `over the seeds and the categories, a second round makes up for the weakest first rounds, about one in six`() {
        // When
        val generations = GENERATIONS.values.flatten()

        // Then
        val secondRounds = generations.count { it.rounds.size > 1 }
        assertThat(secondRounds.toDouble() / generations.size).isBetween(LEAST_SECOND_ROUNDS, MOST_SECOND_ROUNDS)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class, names = ["EPIC", "MELANCHOLIC", "RETRO"])
    fun `over the seeds, the chosen melody leans as its category does no less than the candidates it is chosen from`(
        category: Category,
    ) {
        // Given the moves epic leans to, fifths, melancholic, descents, and retro, arpeggios
        val lean = checkNotNull(category.quality.leaning).lean

        // When
        val generations = GENERATIONS.getValue(category)

        // Then
        val chosen = generations.map { lean.shareOf(it.best.measures()) }.average()
        val candidates = generations.map { generation ->
            generation.rounds.flatten().map { lean.shareOf(it.measures()) }.average()
        }.average()
        assertThat(chosen).isGreaterThanOrEqualTo(candidates)
    }

    @Test
    fun `the seed 42 on joyful gives this music, which pins the derivation of the draws, the note and the threshold`() {
        // When
        val generation = MusicGenerator.choose(Category.JOYFUL, 42)
        val music = MusicGenerator.generate(Category.JOYFUL, 42, AN_ID)

        // Then a first round whose best, 84.7 points, falls under the threshold of joyful, 85.5, then a second round
        // whose best, 90.4 points, plays in A major at 146 beats per minute
        assertThat(generation.rounds).hasSize(2)
        assertThat(generation.rounds.first().maxOf { it.quality.points }).isCloseTo(84.67, within(0.01))
        assertThat(generation.best.quality.points).isCloseTo(90.43, within(0.01))
        assertThat(generation.best).isIn(generation.rounds.last())
        assertThat(music.scale).isEqualTo(Scale(Pitch.parse("A4"), ScaleType.MAJOR))
        assertThat(music.score.bpm).isEqualTo(146)
        val (melody, bass) = music.score.voices.map { voice -> voice.notes.mapNotNull { it.pitch?.toString() } }
        assertThat(melody.take(6)).containsExactly("A4", "B4", "A4", "E4", "F#4", "G#4")
        assertThat(melody.takeLast(4)).containsExactly("B4", "B4", "G#4", "B4")
        assertThat(bass.take(4)).containsExactly("A2", "A3", "A2", "E2")
    }

    @Test
    fun `rounds whose candidates were all ruled out leave no best one, an error sixty ruled out in a row would make`() {
        // Given three rounds without a candidate left
        val rounds = List(3) { emptyList<Candidate>() }

        // When / Then
        assertThatIllegalStateException().isThrownBy { Generation(Category.JOYFUL, rounds) }
    }

    private fun Candidate.measures(): MelodyMeasures = MelodyMeasures(form, melody)

    private companion object {
        /** The share of the generations that compose a second round: their first round falls under the threshold. */
        const val LEAST_SECOND_ROUNDS = 0.05
        const val MOST_SECOND_ROUNDS = 0.3

        /** The identity of the generated musics: the one the music editor gives a music it opens. */
        val AN_ID = ScoreId.Personal("a-music")

        /** The bars of a generated music. */
        const val BARS = 8

        /** Seeds enough to find the rules that depend on the draw, for every category. */
        val SEEDS: List<Long> = (0L until 20L).toList()

        /** The generations of every category over the [SEEDS], composed once for the tests that look at them all. */
        val GENERATIONS: Map<Category, List<Generation>> by lazy {
            Category.entries.associateWith { category -> SEEDS.map { seed -> MusicGenerator.choose(category, seed) } }
        }

        /** Enough seeds for the rules that hold the same way whatever the draw. */
        @JvmStatic
        fun fewSeeds(): List<Long> = (0L until 3L).toList()
    }
}
