package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.placing
import ch.magori.blox.core.music.RomanNumeral
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** What TalkBack reads of the screen beyond its message: the orientation of a piece, the chords heard. */
class LocksmithReadingTest {
    private val start = PadlockGame.start(PADLOCK)

    @Test
    fun `the orientation of a piece is read in quarter turns from where it started, clockwise`() {
        // Given the T selected
        val rank = start.given.indexOf(PieceType.T)
        val selected = start.select(rank)

        // When it is turned
        val turned = generateSequence(selected) { it.turn(Rotation.CLOCKWISE) }.take(4).toList()
        val left = selected.turn(Rotation.COUNTERCLOCKWISE)

        // Then
        assertThat(turned.map { orientationOf(it, rank) }).containsExactly(
            R.string.feature_locksmith_orientation_upright,
            R.string.feature_locksmith_orientation_right,
            R.string.feature_locksmith_orientation_half,
            R.string.feature_locksmith_orientation_left,
        )
        assertThat(orientationOf(left, rank)).isEqualTo(R.string.feature_locksmith_orientation_left)
    }

    @Test
    fun `a piece of two orientations is upright or turned a quarter, either way, and an O always upright`() {
        // Given the I and the O
        val i = start.given.indexOf(PieceType.I)
        val o = start.given.indexOf(PieceType.O)

        // When each is turned once, either way
        val turns = listOf(Rotation.CLOCKWISE, Rotation.COUNTERCLOCKWISE)
        val readings = turns.flatMap { rotation ->
            listOf(orientationOf(start.select(i).turn(rotation), i), orientationOf(start.select(o).turn(rotation), o))
        }

        // Then
        assertThat(orientationOf(start, i)).isEqualTo(R.string.feature_locksmith_orientation_upright)
        assertThat(readings).containsExactly(
            R.string.feature_locksmith_orientation_quarter,
            R.string.feature_locksmith_orientation_upright,
            R.string.feature_locksmith_orientation_quarter,
            R.string.feature_locksmith_orientation_upright,
        )
    }

    @Test
    fun `the band of the chords reads those heard, then those to come`() {
        // Given one piece placed of the three, IV – V – I
        val placed = start.placing(PADLOCK.solution.first())

        // When
        val before = chordsOf(LocksmithUiState(level = 1, game = start))
        val after = chordsOf(LocksmithUiState(level = 1, game = placed))

        // Then
        assertThat(before.heard).isEmpty()
        assertThat(before.coming.map { it.name }).containsExactly("IV", "V", "I")
        assertThat(after.heard.map { it.name }).containsExactly("IV")
        assertThat(after.coming.map { it.name }).containsExactly("V", "I")
    }

    @Test
    fun `a chord is read by its degree and its quality, not by its numeral, which TalkBack would read as a word`() {
        // When
        val names = listOf("I", "vi", "vii°").map { chordNameOf(RomanNumeral.parse(it)) }

        // Then each name takes the degree, 1, 6 and 7
        assertThat(names).containsExactly(
            R.string.feature_locksmith_chord_major,
            R.string.feature_locksmith_chord_minor,
            R.string.feature_locksmith_chord_diminished,
        )
    }

    @Test
    fun `the band says when no chord is heard yet, and when they all are`() {
        // Given
        val solved = PADLOCK.solution.fold(start) { game, piece -> game.placing(piece) }
        val one = start.placing(PADLOCK.solution.first())

        // Then
        assertThat(chordBandOf(chordsOf(LocksmithUiState(level = 1, game = start))))
            .isEqualTo(R.string.feature_locksmith_chords_none_heard)
        assertThat(
            chordBandOf(chordsOf(LocksmithUiState(level = 1, game = one))),
        ).isEqualTo(R.string.feature_locksmith_chords)
        assertThat(chordBandOf(chordsOf(LocksmithUiState(level = 1, game = solved))))
            .isEqualTo(R.string.feature_locksmith_chords_all_heard)
    }

    @Test
    fun `the cadence that closed the phrase is read by its two chords, V then I`() {
        assertThat(cadenceOf(LocksmithUiState(level = 1, game = start)).map { it.name }).containsExactly("V", "I")
    }

    private companion object {
        /** An O, an I and a T: pieces of one, two and four orientations. */
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
    }
}
