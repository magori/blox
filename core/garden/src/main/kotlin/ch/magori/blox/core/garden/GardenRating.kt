package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Piece

/**
 * How well a garden sounds, from which [GardenComposer] keeps the best of its candidates: the cells on a note of the
 * chord of their group, [chordNotes]; the groups holding at least one such note, [chordGroups]; the [types] of its
 * pieces; and the gap in cells between its fullest and its emptiest group, [imbalance]. The [points] weigh them: a first
 * setting, to be tuned by ear.
 */
internal data class GardenRating(val chordNotes: Int, val chordGroups: Int, val types: Int, val imbalance: Int) {
    /** The rating as a whole number, the higher the better. */
    val points: Int =
        CHORD_NOTE_POINTS * chordNotes + CHORD_GROUP_POINTS * chordGroups + TYPE_POINTS * types -
            IMBALANCE_PENALTY * imbalance

    companion object {
        /**
         * Points per cell on a note of the chord of its group: the first criterion, as the notes of the chord are those
         * that sound right over it. A candidate places 24 cells, so their count is their share.
         */
        const val CHORD_NOTE_POINTS: Int = 10

        /**
         * Points per group holding at least one note of its chord, so that each chord of the progression is heard: a
         * group without one is worth three notes of the chord elsewhere.
         */
        const val CHORD_GROUP_POINTS: Int = 30

        /** Points per type of piece: the shapes, and with them the rhythms, vary. */
        const val TYPE_POINTS: Int = 10

        /** Points taken off per cell of gap between the fullest and the emptiest group: the music spreads over the bars. */
        const val IMBALANCE_PENALTY: Int = 5

        /** The rating of [garden], in its mood. */
        fun of(garden: Garden): GardenRating {
            val cells = garden.pieces.flatMap(Piece::cells)
            val chordCells = cells.filter(garden.mood::isChordNote)
            val cellsPerGroup = cells.groupingBy { GardenMood.groupOf(it.column) }.eachCount()
            val filling = (0 until GardenMood.CHORDS).map { cellsPerGroup[it] ?: 0 }
            return GardenRating(
                chordNotes = chordCells.size,
                chordGroups = chordCells.map { GardenMood.groupOf(it.column) }.distinct().size,
                types = garden.pieces.map(Piece::type).distinct().size,
                imbalance = filling.max() - filling.min(),
            )
        }
    }
}
