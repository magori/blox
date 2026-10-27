package ch.magori.blox.feature.locksmith

import androidx.annotation.StringRes
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.music.ChordQuality
import ch.magori.blox.core.music.RomanNumeral

/**
 * What TalkBack reads of the orientation of the given piece of [rank] in [game]: the quarter turns, clockwise, from the
 * orientation it was given in.
 */
@StringRes
internal fun orientationOf(
    game: PadlockGame,
    rank: Int,
): Int {
    val type = game.given[rank]
    val orientations = ClassicRotationSystem.orientations(type)
    val start = orientations.indexOf(ClassicRotationSystem.spawnOrientation(type))
    val turns = (orientations.indexOf(game.orientationOf(rank)) - start).mod(orientations.size)
    return when {
        turns == 0 -> R.string.feature_locksmith_orientation_upright

        // A piece of two orientations, I, S or Z, comes to the same either way.
        orientations.size == 2 -> R.string.feature_locksmith_orientation_quarter

        else -> QUARTER_TURNS[turns]
    }
}

/** The orientations of a piece, by the quarter turns from where it started. */
private val QUARTER_TURNS = listOf(
    R.string.feature_locksmith_orientation_upright,
    R.string.feature_locksmith_orientation_right,
    R.string.feature_locksmith_orientation_half,
    R.string.feature_locksmith_orientation_left,
)

/** The chords of a padlock as its band reads them: those [heard], in order, then those still [coming]. */
internal data class ChordsRead(val heard: List<RomanNumeral>, val coming: List<RomanNumeral>)

/** The chords of the padlock of [state], as its band reads them: one per piece on the grid heard, the others to come. */
internal fun chordsOf(state: LocksmithUiState): ChordsRead {
    val numerals = state.game.progression.numerals
    val heard = state.game.chords.size
    return ChordsRead(heard = numerals.take(heard), coming = numerals.drop(heard))
}

/**
 * How TalkBack names [numeral], with its degree: by its quality, « 6 mineur », rather than by its numeral, « vi », read
 * as a word. The chords of a padlock bear no accidental.
 */
@StringRes
internal fun chordNameOf(numeral: RomanNumeral): Int = when (numeral.quality) {
    ChordQuality.MAJOR -> R.string.feature_locksmith_chord_major
    ChordQuality.MINOR -> R.string.feature_locksmith_chord_minor
    ChordQuality.DIMINISHED -> R.string.feature_locksmith_chord_diminished
}

/** What the band of [chords] says: the chords heard and those to come; none heard yet; or all of them heard. */
@StringRes
internal fun chordBandOf(chords: ChordsRead): Int = when {
    chords.heard.isEmpty() -> R.string.feature_locksmith_chords_none_heard
    chords.coming.isEmpty() -> R.string.feature_locksmith_chords_all_heard
    else -> R.string.feature_locksmith_chords
}

/** The chords of the cadence that closes the phrase of the padlock of [state], V then I: the last [CADENCE]. */
internal fun cadenceOf(state: LocksmithUiState): List<RomanNumeral> = state.game.progression.numerals.takeLast(CADENCE)

/** The chords of the cadence V–I, the last two of a padlock. */
internal const val CADENCE = 2
