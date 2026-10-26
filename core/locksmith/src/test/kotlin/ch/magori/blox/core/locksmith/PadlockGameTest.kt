package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.music.Chord
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Progression
import ch.magori.blox.core.music.RomanNumeral
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PadlockGameTest {
    @Test
    fun `a new game selects the first given piece in its spawn orientation, nothing placed`() {
        // When
        val game = PadlockGame.start(PADLOCK)

        // Then
        assertThat(game.selected).isEqualTo(0)
        assertThat(game.given).containsExactly(PieceType.I, PieceType.O, PieceType.T)
        assertThat(game.orientationOf(0)).isEqualTo(ClassicRotationSystem.spawnOrientation(PieceType.I))
        assertThat(game.placed).isEmpty()
    }

    @Test
    fun `a piece inside the hole is placed, its pivot on the cell aimed at`() {
        // Given: the I selected, flat
        val game = PadlockGame.start(PADLOCK)

        // When: its pivot on column 5, row 1, it covers columns 3 to 6 of row 1
        val update = game.place(Position(5, 1))

        // Then
        val piece = Piece(PieceType.I, ClassicRotationSystem.spawnOrientation(PieceType.I), Position(5, 1))
        assertThat(update.events).containsExactly(PadlockEvent.PiecePlaced(rank = 0, piece = piece))
        assertThat(update.game.placed).containsExactlyEntriesOf(mapOf(0 to piece))
        assertThat(update.game.moves).isEqualTo(1)
        assertThat(update.game.refusals).isZero
    }

    @Test
    fun `a piece over the metal is refused as out of the hole and only the refusals count it`() {
        // Given: the T turned, to check that nothing else changes
        val game = PadlockGame.start(PADLOCK).select(2).turn(Rotation.CLOCKWISE)

        // When: the T on rows 5 and 6 lies on the metal
        val update = game.place(Position(4, 5))

        // Then
        assertThat(update.events).containsExactly(PadlockEvent.PlacementRefused(rank = 2, reason = Refusal.OUT_OF_HOLE))
        assertThat(update.game.placed).isEmpty()
        assertThat(update.game.selected).isEqualTo(2)
        assertThat(update.game.given.indices.map(update.game::orientationOf))
            .isEqualTo(game.given.indices.map(game::orientationOf))
        assertThat(update.game.moves).isZero
        assertThat(update.game.refusals).isEqualTo(1)
    }

    @Test
    fun `a piece off the grid is refused as out of the hole`() {
        // When: the flat I with its pivot in the corner reaches columns -2 and -1
        val update = PadlockGame.start(PADLOCK).place(Position(0, 0))

        // Then
        assertThat(update.events).containsExactly(PadlockEvent.PlacementRefused(rank = 0, reason = Refusal.OUT_OF_HOLE))
    }

    @Test
    fun `the first free piece is selected after a placement`() {
        // Given: the O selected first
        val game = PadlockGame.start(PADLOCK).select(1)

        // When: the O is placed where the solution has it
        val update = game.place(Position(2, 1))

        // Then: the I, rank 0, is the first free piece
        assertThat(update.game.selected).isEqualTo(0)
    }

    @Test
    fun `a piece over a placed one is refused as an overlap`() {
        // Given: the I placed, the O selected
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game

        // When: the O covers columns 2 and 3 of rows 1 and 2, the I holds (3, 1)
        val update = game.place(Position(3, 1))

        // Then
        assertThat(update.events).containsExactly(PadlockEvent.PlacementRefused(rank = 1, reason = Refusal.OVERLAP))
        assertThat(update.game.placed).containsOnlyKeys(0)
        assertThat(update.game.selected).isEqualTo(1)
    }

    @Test
    fun `a piece both over the metal and over a placed one is refused as out of the hole`() {
        // Given: the I placed, the O selected
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game

        // When: the O covers (3, 0) on the metal and (3, 1) under the I
        val update = game.place(Position(4, 0))

        // Then
        assertThat(update.events).containsExactly(PadlockEvent.PlacementRefused(rank = 1, reason = Refusal.OUT_OF_HOLE))
    }

    @Test
    fun `turning turns the selected piece a quarter turn either way`() {
        // Given: the T selected
        val game = PadlockGame.start(PADLOCK).select(2)

        // When
        val clockwise = game.turn(Rotation.CLOCKWISE)
        val back = clockwise.turn(Rotation.COUNTERCLOCKWISE)

        // Then
        assertThat(clockwise.orientationOf(2)).isEqualTo(T_TURNED)
        assertThat(back.orientationOf(2)).isEqualTo(T_SPAWN)
    }

    @Test
    fun `each piece keeps its orientation when another one is selected`() {
        // Given: the T turned once, then the I selected
        val game = PadlockGame.start(PADLOCK).select(2).turn(Rotation.CLOCKWISE).select(0)

        // When
        val back = game.select(2)

        // Then
        assertThat(back.orientationOf(2)).isEqualTo(T_TURNED)
        assertThat(back.orientationOf(0)).isEqualTo(ClassicRotationSystem.spawnOrientation(PieceType.I))
    }

    @Test
    fun `a placed piece is taken back, selected in its orientation, and the moves are kept`() {
        // Given: the T turned twice, pointing up, placed on (3, 2) to (5, 2) and (4, 1)
        val game = PadlockGame.start(PADLOCK).select(2).turn(Rotation.CLOCKWISE).turn(Rotation.CLOCKWISE)
            .place(Position(4, 2)).game

        // When
        val update = game.takeBack(2)

        // Then
        assertThat(update.events).containsExactly(PadlockEvent.PieceTakenBack(rank = 2))
        assertThat(update.game.placed).isEmpty()
        assertThat(update.game.selected).isEqualTo(2)
        assertThat(update.game.orientationOf(2)).isEqualTo(game.orientationOf(2)).isNotEqualTo(T_SPAWN)
        assertThat(update.game.moves).isEqualTo(1)
    }

    @Test
    fun `selecting a placed piece or taking back a free one changes nothing`() {
        // Given: the I placed
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game

        // Then
        assertThat(game.select(0)).isEqualTo(game)
        assertThat(game.takeBack(1)).isEqualTo(PadlockUpdate(game, emptyList()))
    }

    @ParameterizedTest(name = "rank {0}")
    @ValueSource(ints = [-1, 3])
    fun `a rank that is not given is a programming error`(rank: Int) {
        // Given
        val game = PadlockGame.start(PADLOCK)

        // Then
        assertThatIllegalArgumentException().isThrownBy { game.select(rank) }
        assertThatIllegalArgumentException().isThrownBy { game.orientationOf(rank) }
        assertThatIllegalArgumentException().isThrownBy { game.takeBack(rank) }
    }

    @Test
    fun `the order of the placements is kept, a piece taken back leaves it`() {
        // Given: the O, then the I placed
        val game = PadlockGame.start(PADLOCK).select(1).place(Position(2, 1)).game
            .place(Position(5, 1)).game

        // When
        val taken = game.takeBack(1).game

        // Then
        assertThat(game.placementOrder).containsExactly(1, 0)
        assertThat(taken.placementOrder).containsExactly(0)
    }

    @Test
    fun `a piece taken back and placed again is the last placed`() {
        // Given: the O, then the I placed, then the O taken back
        val game = PadlockGame.start(PADLOCK).select(1).place(Position(2, 1)).game
            .place(Position(5, 1)).game.takeBack(1).game

        // When
        val again = game.place(Position(2, 1)).game

        // Then
        assertThat(again.placementOrder).containsExactly(0, 1)
    }

    @Test
    fun `two games whose pieces were placed in a different order are not equal`() {
        // When
        val oFirst = PadlockGame.start(PADLOCK).select(1).place(Position(2, 1)).game.place(Position(5, 1)).game
        val iFirst = PadlockGame.start(PADLOCK).place(Position(5, 1)).game.place(Position(2, 1)).game

        // Then
        assertThat(oFirst).isNotEqualTo(iFirst)
    }

    @Test
    fun `placing every piece opens the padlock, the last placement first`() {
        // Given: the I and the O placed, the T selected
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game.place(Position(2, 1)).game

        // When
        val update = game.place(Position(4, 2))

        // Then
        assertThat(update.events).containsExactly(
            PadlockEvent.PiecePlaced(rank = 2, piece = Piece(PieceType.T, T_SPAWN, Position(4, 2))),
            PadlockEvent.PadlockOpened,
        )
        assertThat(update.game.isOpen).isTrue
        assertThat(update.game.selected).isNull()
    }

    @Test
    fun `once the padlock is open nothing but restart is taken`() {
        // Given
        val open = PadlockGame.start(PADLOCK).place(Position(5, 1)).game.place(Position(2, 1)).game
            .place(Position(4, 2)).game

        // Then
        assertThat(open.select(0)).isEqualTo(open)
        assertThat(open.turn(Rotation.CLOCKWISE)).isEqualTo(open)
        assertThat(open.place(Position(4, 2))).isEqualTo(PadlockUpdate(open, emptyList()))
        assertThat(open.takeBack(0)).isEqualTo(PadlockUpdate(open, emptyList()))
        assertThat(open.restart().isOpen).isFalse
    }

    @Test
    fun `restarting keeps the padlock, clears the pieces placed and the counters`() {
        // Given: the I placed, a refusal, a hint, the T turned
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game.place(Position(1, 1)).game
            .hint().game.select(2).turn(Rotation.CLOCKWISE)

        // When
        val restarted = game.restart()

        // Then
        assertThat(restarted).isEqualTo(PadlockGame.start(PADLOCK))
    }

    @Test
    fun `a game plays the chords of its number of pieces`() {
        assertThat(PadlockGame.start(PADLOCK).progression).isEqualTo(Progression.parse("IV-V-I"))
        assertThat(PadlockGame.start(STRIP).progression).isEqualTo(Progression.parse("I-IV-V-I"))
    }

    @Test
    fun `the chords heard are one per piece on the grid, from C4 major, the last placement resolving on I`() {
        // Given: a padlock of 4 pieces, I-IV-V-I
        val start = PadlockGame.start(STRIP)

        // When
        val one = start.place(Position(3, 1)).game
        val open = one.place(Position(3, 2)).game.place(Position(6, 1)).game.place(Position(8, 1)).game

        // Then
        assertThat(start.chords).isEmpty()
        assertThat(one.chords).containsExactly(chord("I"))
        assertThat(one.chords.single().root).isEqualTo(Pitch.parse("C4"))
        assertThat(open.isOpen).isTrue
        assertThat(open.chords).containsExactly(chord("I"), chord("IV"), chord("V"), chord("I"))
    }

    @Test
    fun `a piece taken back takes its chord back`() {
        // Given: two pieces placed, IV then V
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game.place(Position(2, 1)).game

        // When
        val taken = game.takeBack(0).game

        // Then
        assertThat(taken.chords).containsExactly(chord("IV"))
    }

    @Test
    fun `a cell tells the rank of the placed piece that covers it, if any`() {
        // Given: the I placed on row 1, columns 3 to 6
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game

        // Then
        assertThat(game.rankAt(Position(4, 1))).isEqualTo(0)
        assertThat(game.rankAt(Position(2, 1))).isNull()
    }

    @Test
    fun `the selected piece put down on a cell is its type, in its orientation, its pivot there`() {
        // Given: the T selected and turned
        val game = PadlockGame.start(PADLOCK).select(2).turn(Rotation.CLOCKWISE)

        // When
        val piece = game.selectedAt(Position(4, 4))

        // Then
        assertThat(piece).isEqualTo(Piece(PieceType.T, T_TURNED, Position(4, 4)))
        assertThat(game.place(Position(4, 4)).events.single()).isInstanceOf(PadlockEvent.PlacementRefused::class.java)
    }

    @Test
    fun `an open padlock has no selected piece to put down`() {
        // Given
        val open = PadlockGame.start(PADLOCK).place(Position(5, 1)).game.place(Position(2, 1)).game
            .place(Position(4, 2)).game

        // Then
        assertThat(open.selectedAt(Position(4, 4))).isNull()
    }

    private companion object {
        /** Two I on the left, two O on the right; given as I, I, O, O. */
        val STRIP = Padlock.parse(
            """
            ##########
            #11113344#
            #22223344#
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        fun chord(numeral: String): Chord = Scale(Pitch.parse("C4"), ScaleType.MAJOR).chord(RomanNumeral.parse(numeral))

        /** O on rank 1, I on rank 2, T on rank 3; given as I, O, T. */
        val PADLOCK = Padlock.parse(
            """
            ##########
            #112222###
            #11333####
            ####3#####
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        val T_SPAWN = ClassicRotationSystem.spawnOrientation(PieceType.T)

        val T_TURNED = ClassicRotationSystem.rotate(PieceType.T, T_SPAWN, Rotation.CLOCKWISE)
    }
}
