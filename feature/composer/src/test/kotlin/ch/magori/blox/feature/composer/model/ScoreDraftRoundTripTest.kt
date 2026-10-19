package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.MusicGenerator
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Scores
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ScoreDraftRoundTripTest {
    @ParameterizedTest
    @EnumSource(Category::class)
    fun `every music of the generator opens on its scale and comes back unchanged`(category: Category) {
        (1L..5L).forEach { seed ->
            // Given
            val generated = MusicGenerator.generate(category, seed, ScoreId.Personal("generated"))

            // When
            val reopened = ScoreDraft.fromScore(generated.score, generated.scale, generated.category)
                ?.toScore(generated.score.id)

            // Then
            assertThat(reopened).describedAs("seed %d", seed).isEqualTo(generated.score)
        }
    }

    @Test
    fun `a generated music with sixteenth notes, dotted eighths and rests comes back unchanged`() {
        // Given a Retro-like melody and an eighth-note bass, written as the generator writes them
        val gMixolydian = Scale(Pitch.parse("G4"), ScaleType.MIXOLYDIAN)
        val generated = score(
            melody = thenSilence(
                notes("G4:0.25 B4:0.25 D5:0.25 G5:0.25 F5:0.5 D5:0.5 C5:0.25 D5:0.25 E5:0.25 F5:0.25 G5:1") +
                    notes("A4:0.75 B4:0.25 C5:0.75 D5:0.25 E5:1 D5:1") +
                    notes("C5:2 B4:1 r:1"),
                bars = 5,
            ),
            bass = thenSilence(notes("G2:0.5 G3:0.5 G2:0.5 G3:0.5 F2:0.5 F3:0.5 F2:0.5 F3:0.5"), bars = 7),
            bpm = 156,
        )

        // When
        val reopened = ScoreDraft.fromScore(generated, gMixolydian, Category.RETRO)?.toScore(generated.id)

        // Then
        assertThat(reopened).isEqualTo(generated)
    }

    @Test
    fun `the arranged Korobeiniki opens and comes back unchanged`() {
        // Given
        val aHarmonicMinor = Scale(Pitch.parse("A4"), ScaleType.HARMONIC_MINOR)

        // When
        val reopened = ScoreDraft.fromScore(Scores.korobeiniki, aHarmonicMinor, null)?.toScore(Scores.korobeiniki.id)

        // Then
        assertThat(reopened).isEqualTo(Scores.korobeiniki)
    }

    @Test
    fun `a score comes back with its silences written one rest per bar, the music heard unchanged`() {
        // Given two rests side by side in the first bar, then a rest across the line between the next two bars
        val cMajor = Scale(Pitch.parse("C4"), ScaleType.MAJOR)
        val written = score(thenSilence(notes("E4:1 r:1 r:1 E4:1 E4:3 r:2 E4:3"), bars = 5), silence())

        // When
        val reopened = ScoreDraft.fromScore(written, cMajor, null)?.toScore(written.id)

        // Then the two rests are one, and the rest across the bar line is cut there
        assertThat(reopened?.voices?.first()?.notes).containsExactlyElementsOf(
            thenSilence(notes("E4:1 r:2 E4:1 E4:3 r:1 r:1 E4:3"), bars = 5),
        )
    }
}
