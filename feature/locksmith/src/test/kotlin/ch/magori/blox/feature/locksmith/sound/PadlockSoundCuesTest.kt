package ch.magori.blox.feature.locksmith.sound

import ch.magori.blox.core.audio.SoundCue
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockUpdate
import ch.magori.blox.core.locksmith.placing
import ch.magori.blox.core.locksmith.solved
import ch.magori.blox.core.locksmith.turnedTo
import ch.magori.blox.core.music.LocksmithSounds
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Progression
import ch.magori.blox.core.music.RomanNumeral
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PadlockSoundCuesTest {
    private val start = PadlockGame.start(Padlock.generate(level = 2, seed = 7))

    /** The first given piece, where the solution has it. */
    private val firstPiece = start.padlock.solution.first { it.type == start.given.first() }

    @Test
    fun `a placement plays the arpeggio and the bass of its chord`() {
        // Given
        val placement = start.turnedTo(firstPiece.orientation).place(firstPiece.pivot)

        // When
        val cues = soundCuesFor(placement)

        // Then
        val chord = placement.game.chords.last()
        assertThat(cues).containsExactly(
            SoundCue(LocksmithSounds.arpeggio(chord)),
            SoundCue(LocksmithSounds.bass(chord)),
        )
    }

    @Test
    fun `the second placement plays the chord of IV`() {
        // Given: a padlock of 4 pieces, I-IV-V-I, its first piece placed
        val afterFirst = start.placing(firstPiece)
        val second = start.padlock.solution.first { it != firstPiece }

        // When
        val cues = soundCuesFor(afterFirst.updatePlacing(second))

        // Then
        val iv = Scale(Pitch.parse("C4"), ScaleType.MAJOR).chord(RomanNumeral.parse("IV"))
        assertThat(start.progression).isEqualTo(Progression.parse("I-IV-V-I"))
        assertThat(cues).containsExactly(SoundCue(LocksmithSounds.arpeggio(iv)), SoundCue(LocksmithSounds.bass(iv)))
    }

    @Test
    fun `the last placement adds the opening flourish after the final chord`() {
        // Given: every piece but the last one placed
        val solution = start.padlock.solution
        val beforeLast = solution.dropLast(1).fold(start) { game, piece -> game.placing(piece) }

        // When
        val opening = beforeLast.updatePlacing(solution.last())
        val cues = soundCuesFor(opening)

        // Then
        val finalChord = opening.game.chords.last()
        assertThat(cues).containsExactly(
            SoundCue(LocksmithSounds.arpeggio(finalChord)),
            SoundCue(LocksmithSounds.bass(finalChord)),
            SoundCue(LocksmithSounds.openingFlourish, afterMillis = 450),
        )
    }

    @Test
    fun `the flourish waits for the final chord to end`() {
        // Given
        val opening = start.padlock.solution.dropLast(1).fold(start) { game, piece -> game.placing(piece) }
            .updatePlacing(start.padlock.solution.last())

        // When
        val flourish = soundCuesFor(opening).single { it.effect == LocksmithSounds.openingFlourish }

        // Then
        val finalArpeggio = LocksmithSounds.arpeggio(opening.game.chords.last())
        assertThat(flourish.afterMillis).isGreaterThanOrEqualTo(finalArpeggio.millis.toLong())
    }

    @Test
    fun `a refusal plays the refusal`() {
        // Given: the pivot in the corner, on the metal
        val refusal = start.place(Position(0, 0))

        // When
        val cues = soundCuesFor(refusal)

        // Then
        assertThat(cues).containsExactly(SoundCue(LocksmithSounds.refusal))
    }

    @Test
    fun `taking back and a hint play nothing`() {
        // Given
        val placed = start.placing(firstPiece)

        // When
        val takingBack = soundCuesFor(placed.takeBack(0))
        val hint = soundCuesFor(start.hint())

        // Then
        assertThat(takingBack).isEmpty()
        assertThat(hint).isEmpty()
    }

    @Test
    fun `the effects warmed up are those of every chord of the padlock, the flourish and the refusal, once each`() {
        // When
        val effects = warmUpEffectsOf(start)

        // Then: the tonic, heard first and last, is warmed up once
        val chords = start.solved().chords
        assertThat(effects).containsExactlyInAnyOrderElementsOf(
            (chords.flatMap(LocksmithSounds::effectsOf) + LocksmithSounds.openingFlourish + LocksmithSounds.refusal)
                .distinct(),
        )
    }

    /** The update of this game as [piece] is placed: a free given piece of its type selected, turned as it, put down. */
    private fun PadlockGame.updatePlacing(piece: Piece): PadlockUpdate {
        val rank = given.indices.first { it !in placed && given[it] == piece.type }
        return select(rank).turnedTo(piece.orientation).place(piece.pivot)
    }
}
