package ch.magori.blox.feature.composer.model

/** Why the save dialog cannot save the music. */
internal enum class SaveProblem {
    /** Neither grid has a note. */
    EMPTY_GRID,

    /** Another music has that name; the dialog offers a free one, [MusicNames.free]. */
    NAME_TAKEN,

    /** The name is empty, or only spaces. */
    BLANK_NAME,
}
