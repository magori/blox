package ch.magori.blox.feature.composer.model

/** What touching a cell does: place or erase a note, or lengthen the note on its left. */
internal enum class EditMode {
    PLACE,
    LENGTHEN,
}
