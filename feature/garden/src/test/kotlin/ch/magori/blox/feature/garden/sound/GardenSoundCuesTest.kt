package ch.magori.blox.feature.garden.sound

import ch.magori.blox.core.audio.SoundCue
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.music.GardenSounds
import ch.magori.blox.core.music.Pitch
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GardenSoundCuesTest {
    @Test
    fun `a preview plays its notes once, in a rising arpeggio, one note every 80 ms`() {
        // Given: a T pointing right, in the calm mood, over rows 9 to 11: A3 on top, G3 twice in the middle, E3 below
        val t = Piece(PieceType.T, ClassicRotationSystem.orientations(PieceType.T)[1], Position(5, 10))

        // When
        val cues = previewCuesOf(t, GardenMood.CALM)

        // Then: each note once, from the lowest, 80 ms after the one before
        assertThat(cues).containsExactly(
            SoundCue(GardenSounds.note(Pitch.parse("E3")), afterMillis = 0),
            SoundCue(GardenSounds.note(Pitch.parse("G3")), afterMillis = 80),
            SoundCue(GardenSounds.note(Pitch.parse("A3")), afterMillis = 160),
        )
    }

    @Test
    fun `a standing piece plays its notes from the lowest, at the bottom of the grid, to the highest`() {
        // Given: an I standing in column 3, rows 10 to 13, in the joyful mood: do 4, ré 4, mi 4, fa 4 from the bottom
        val i = Piece(PieceType.I, ClassicRotationSystem.orientations(PieceType.I)[0], Position(3, 12))

        // When
        val cues = previewCuesOf(i, GardenMood.JOYFUL)

        // Then
        assertThat(cues.map { it.effect }).containsExactlyElementsOf(
            i.cells.sortedByDescending { it.row }.map { GardenSounds.note(GardenMood.JOYFUL.pitchAt(it.row)) },
        )
    }

    @Test
    fun `a note held by several cells of the preview plays once`() {
        // Given: an I lying on the bottom row, four cells on one note
        val i = Piece(PieceType.I, ClassicRotationSystem.spawnOrientation(PieceType.I), Position(5, 13))

        // When
        val cues = previewCuesOf(i, GardenMood.CALM)

        // Then
        assertThat(cues).containsExactly(SoundCue(GardenSounds.note(Pitch.parse("C3")), afterMillis = 0))
    }

    @Test
    fun `the cells of a preview off the grid play nothing, the others play`() {
        // Given: an I standing on the bottom row, its pivot there, two cells under the grid
        val standing = ClassicRotationSystem.orientations(PieceType.I)[0]
        val i = Piece(PieceType.I, standing, Position(0, 13))
        val inside = i.cells.filter { it.row < Garden.ROWS }

        // When
        val cues = previewCuesOf(i, GardenMood.JOYFUL)

        // Then
        assertThat(inside).isNotEmpty.hasSizeLessThan(4)
        assertThat(cues).hasSameSizeAs(inside)
    }

    @Test
    fun `the effects warmed up are one note per pitch of the garden, 25 in all`() {
        // When / Then
        assertThat(GARDEN_WARM_UP).hasSize(25)
        assertThat(GARDEN_WARM_UP).containsExactlyInAnyOrderElementsOf(GardenMood.allPitches.map(GardenSounds::note))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(GardenMood::class)
    fun `every note a preview may play, in any mood, is one of the effects warmed up`(mood: GardenMood) {
        // Given: a vertical I in each column, its four rows covering the grid from the top down
        val standing = ClassicRotationSystem.orientations(PieceType.I)[0]
        val previews = (0 until Garden.ROWS).map { row -> Piece(PieceType.I, standing, Position(0, row)) }

        // When
        val effects = previews.flatMap { previewCuesOf(it, mood) }.map { it.effect }.toSet()

        // Then every row was heard, each from the effects warmed up
        assertThat(effects).hasSize(Garden.ROWS)
        assertThat(GARDEN_WARM_UP).containsAll(effects)
    }

    @Test
    fun `a note out of the effects warmed up is a bug, refused rather than played on a new track`() {
        // Given: effects warmed up without the do of the bottom row
        val i = Piece(PieceType.I, ClassicRotationSystem.spawnOrientation(PieceType.I), Position(5, 13))
        val warmed = GardenMood.allPitches - Pitch.parse("C3")

        // When / Then
        assertThatIllegalStateException().isThrownBy { previewCuesOf(i, GardenMood.CALM, warmed) }
    }
}
