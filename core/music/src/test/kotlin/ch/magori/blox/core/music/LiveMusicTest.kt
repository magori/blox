package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

/** The two moods of a live music, composed together for a game from its seed. */
class LiveMusicTest {
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `a live music is a calm music and a tense one`(seed: Long) {
        // When
        val music = LiveMusic.compose(seed)

        // Then
        assertThat(music.calm.category).isEqualTo(Category.CALM)
        assertThat(music.tense.category).isEqualTo(Category.EPIC)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `the calm and the tense music have the same number of bars`(seed: Long) {
        // When
        val music = LiveMusic.compose(seed)

        // Then each voice of both spans the same bars, so that one takes over from the other at the same rank
        val (calm, tense) = listOf(music.calm.score, music.tense.score).map { score ->
            score.voices.map { voice -> voice.beats / score.beatsPerBar }
        }
        assertThat(tense).containsOnly(tense.first())
        assertThat(calm).containsOnly(tense.first())
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `the calm and the tense music are known by their ids, the same from one game to the next`(seed: Long) {
        // When
        val music = LiveMusic.compose(seed)

        // Then
        assertThat(music.calm.score.id).isEqualTo(LiveMusic.CALM_ID)
        assertThat(music.tense.score.id).isEqualTo(LiveMusic.TENSE_ID)
        assertThat(LiveMusic.CALM_ID).isNotEqualTo(LiveMusic.TENSE_ID)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `the calm and the tense music share their tonic`(seed: Long) {
        // When
        val music = LiveMusic.compose(seed)

        // Then
        assertThat(music.tense.scale.tonic).isEqualTo(music.calm.scale.tonic)
    }

    @Test
    fun `two musics of different tonics are not a live music`() {
        // Given a calm music in D and a tense one in G
        val calm = MusicGenerator.generate(Category.CALM, 0, LiveMusic.CALM_ID, Pitch.parse("D4"))
        val tense = MusicGenerator.generate(Category.EPIC, 0, LiveMusic.TENSE_ID, Pitch.parse("G4"))

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { LiveMusic(calm, tense) }.withMessageContaining("D4", "G4")
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `the same seed gives the same two musics`(seed: Long) {
        // When
        val first = LiveMusic.compose(seed)
        val again = LiveMusic.compose(seed)
        val next = LiveMusic.compose(seed + 1)

        // Then
        assertThat(again).isEqualTo(first)
        assertThat(next.calm.score).isNotEqualTo(first.calm.score)
        assertThat(next.tense.score).isNotEqualTo(first.tense.score)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `the calm music does not depend on the tense one`(seed: Long) {
        // When
        val music = LiveMusic.compose(seed)

        // Then the calm one is composed first, from the first seed drawn and on a tonic of its own, as the generator gives
        // it alone; the tense one from a seed of its own
        val first = Random(seed).nextLong()
        assertThat(music.calm).isEqualTo(MusicGenerator.generate(Category.CALM, first, LiveMusic.CALM_ID))
        assertThat(music.tense.seed).isNotEqualTo(first)
    }

    private companion object {
        /** Enough seeds for the rules that hold whatever the draw. */
        @JvmStatic
        fun seeds(): List<Long> = (0L until 5L).toList()
    }
}
