package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CandidateTest {
    @Test
    fun `a form that no melody fits leaves no candidate, rather than an error`() {
        // Given a joyful form in C major, with an F sharp major chord, none of whose notes is in the scale, in bar 8
        val form = Form.compose(Category.JOYFUL, Random(JOYFUL_IN_C)).let { form ->
            val chord = Chord(Pitch.parse("F#4"), ChordQuality.MAJOR)
            form.copy(bars = form.bars.mapIndexed { bar, it -> if (bar == 7) it.copy(chords = listOf(chord)) else it })
        }

        // When
        val candidate = Candidate.compose(form, Random(0))

        // Then
        assertThat(candidate).isNull()
    }

    @Test
    fun `over the seeds, a candidate of mysterious ruled out has a melody, but no bass that fits it`() {
        // Given the forms of mysterious, whose held roots leave the bass no choice
        val composed = (0L until 100L).map { seed ->
            val random = Random(seed)
            val form = Form.compose(Category.MYSTERIOUS, random)
            Triple(seed, form, Candidate.compose(form, random))
        }

        // When
        val ruledOut = composed.filter { (_, _, candidate) -> candidate == null }

        // Then the same draws give each of them a melody, but no bass
        assertThat(ruledOut).isNotEmpty().allSatisfy { (seed, form, _) ->
            val random = Random(seed).also { Form.compose(Category.MYSTERIOUS, it) }
            val melody = Melody.compose(form, random)
            assertThat(melody).isNotNull()
            assertThat(Bass.compose(form, checkNotNull(melody), random)).isNull()
        }
    }

    private companion object {
        /** The seed whose joyful form plays in C major. */
        const val JOYFUL_IN_C = 42L
    }
}
