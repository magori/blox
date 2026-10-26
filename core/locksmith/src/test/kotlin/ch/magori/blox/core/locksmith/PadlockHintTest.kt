package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PadlockHintTest {
    @Test
    fun `the hint shows where a free piece fits and counts a hint`() {
        // Given: nothing placed; the I cannot cover (1, 1) and leave a way, the O can
        val game = PadlockGame.start(PADLOCK)

        // When
        val update = game.hint()

        // Then
        val hint = Hint.Fit(rank = 1, piece = piece(PieceType.O, Position(2, 1)))
        assertThat(update.events).containsExactly(PadlockEvent.HintGiven(hint))
        assertThat(update.game.hints).isEqualTo(1)
    }

    @Test
    fun `the hint never names a piece already placed`() {
        // Given: the I placed where the solution has it
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game

        // When
        val update = game.hint()

        // Then: the O, rank 1, not the I, rank 0
        assertThat(update.events).containsExactly(
            PadlockEvent.HintGiven(Hint.Fit(rank = 1, piece = piece(PieceType.O, Position(2, 1)))),
        )
    }

    @Test
    fun `the hint finds a way even when the player left the solution`() {
        // Given: the O placed on the left, where the solution has the two I
        val game = PadlockGame.start(TWO_WAYS).select(2).place(Position(2, 1)).game

        // When
        val update = game.hint()

        // Then: the first I on row 1, columns 3 to 6
        assertThat(update.events).containsExactly(
            PadlockEvent.HintGiven(Hint.Fit(rank = 0, piece = piece(PieceType.I, Position(5, 1)))),
        )
    }

    @Test
    fun `the hint names the piece that blocks when no way is left`() {
        // Given: an I placed on row 1, columns 2 to 5: (1, 1) can no longer be covered
        val game = PadlockGame.start(TWO_WAYS).place(Position(4, 1)).game

        // When
        val update = game.hint()

        // Then
        assertThat(update.events).containsExactly(PadlockEvent.HintGiven(Hint.Blocking(rank = 0)))
        assertThat(update.game.hints).isEqualTo(1)
    }

    @Test
    fun `the hint names the latest piece whose removal alone leaves a way`() {
        // Given: the I of rank 1 on row 1, columns 2 to 5, then the I of rank 0 on row 2, columns 3 to 6
        val game = PadlockGame.start(TWO_WAYS).select(1).place(Position(4, 1)).game.place(Position(5, 2)).game

        // When
        val update = game.hint()

        // Then: without the latest one (rank 0), (1, 1) still cannot be covered; without rank 1, it can
        assertThat(update.events).containsExactly(PadlockEvent.HintGiven(Hint.Blocking(rank = 1)))
    }

    @Test
    fun `when no single piece unblocks the padlock the hint names the last piece placed`() {
        // Given: both I one column to the right, on rows 1 and 2, the I of rank 1 last
        val game = PadlockGame.start(TWO_WAYS).place(Position(4, 1)).game.place(Position(4, 2)).game

        // When
        val update = game.hint()

        // Then
        assertThat(update.events).containsExactly(PadlockEvent.HintGiven(Hint.Blocking(rank = 1)))
    }

    @Test
    fun `once the padlock is open no hint is given`() {
        // Given
        val open = PadlockGame.start(PADLOCK).place(Position(5, 1)).game.place(Position(2, 1)).game
            .place(Position(4, 2)).game

        // Then
        assertThat(open.hint()).isEqualTo(PadlockUpdate(open, emptyList()))
    }

    @Test
    fun `the hint names the latest of several pieces whose removal alone leaves a way`() {
        // Given: an I on row 1, columns 1 to 4 (the I on the left, the O on the right), then an I on
        // row 2, columns 3 to 6 (an O on the left, the I in the middle): each alone leaves a way
        val game = PadlockGame.start(STRIP).place(Position(3, 1)).game.select(1).place(Position(5, 2)).game

        // When
        val update = game.hint()

        // Then: the latest one, rank 1
        assertThat(update.events).containsExactly(PadlockEvent.HintGiven(Hint.Blocking(rank = 1)))
    }

    @Test
    fun `the hint tries the orientations of a piece in their order`() {
        // Given: four I fill a square, upright or flat; the Classic I lists upright first
        val game = PadlockGame.start(SQUARE)

        // When
        val update = game.hint()

        // Then
        val upright = ClassicRotationSystem.orientations(PieceType.I).first()
        assertThat(update.events).containsExactly(
            PadlockEvent.HintGiven(Hint.Fit(rank = 0, piece = Piece(PieceType.I, upright, Position(1, 3)))),
        )
    }

    @Test
    fun `the hint covers the first free cell in reading order`() {
        // Given: row 1 starts at column 3, row 2 at column 1
        val game = PadlockGame.start(STEP)

        // When
        val update = game.hint()

        // Then: the O on (3, 1), not on (1, 2), first column by column
        assertThat(update.events).containsExactly(
            PadlockEvent.HintGiven(Hint.Fit(rank = 1, piece = piece(PieceType.O, Position(4, 1)))),
        )
    }

    @Test
    @Timeout(5)
    fun `a hint is quick once a piece is placed anywhere in a padlock of 6 pieces`() {
        // Given: on 100 padlocks of 6 pieces, the first given piece placed wherever it first fits, turned or not
        val games = (1..100L).map { seed ->
            val start = PadlockGame.start(PadlockGenerator.generate(6, seed))
            val turned = generateSequence(start) { it.turn(Rotation.CLOCKWISE) }.take(4)
            turned.flatMap { game -> game.padlock.hole.asSequence().map { game.place(it).game } }
                .first { it.placed.isNotEmpty() }
        }

        // When
        val hints = games.map { it.hint().events.single() }

        // Then
        assertThat(hints).hasSize(100)
    }

    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [3, 4, 5, 6])
    fun `following the hints opens every padlock of 200 seeds`(count: Int) {
        // Given
        val games = (1..200L).map { seed -> PadlockGame.start(PadlockGenerator.generate(count, seed)) }

        // When: each hint is followed, its piece selected, turned as shown and placed
        val ends = games.map { start ->
            generateSequence(start) { game ->
                if (game.isOpen) return@generateSequence null
                val fit = (game.hint().events.single() as PadlockEvent.HintGiven).hint as Hint.Fit
                game.select(fit.rank).turnedTo(fit.piece.orientation).place(fit.piece.pivot).game
            }.last()
        }

        // Then
        assertThat(ends).allSatisfy { assertThat(it.isOpen).isTrue }
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

        /** Four flat I in a square; given as I, I, I, I. */
        val SQUARE = Padlock.parse(
            """
            ##########
            #1111#####
            #2222#####
            #3333#####
            #4444#####
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        /** An O on the right of row 1, an O on the left of rows 2 and 3, an I on row 4; given as I, O, O. */
        val STEP = Padlock.parse(
            """
            ##########
            ###11#####
            #2211#####
            #22#######
            #3333#####
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        /** Two I on rows 1 and 2, then an O on the right; given as I, I, O. The O may also go on the left. */
        val TWO_WAYS = Padlock.parse(
            """
            ##########
            #111133###
            #222233###
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

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

        fun piece(
            type: PieceType,
            pivot: Position,
        ): Piece = Piece(type, ClassicRotationSystem.spawnOrientation(type), pivot)
    }
}
