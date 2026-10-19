package ch.magori.blox.feature.composer.model

/** Where the music of the editor stands against its saved version: the info line says « modifiée » or « non enregistrée ». */
internal enum class SaveStatus {
    /** As it was saved. */
    SAVED,

    /** Saved, then changed. */
    MODIFIED,

    /** Never saved: a new music. */
    NOT_SAVED,
}
