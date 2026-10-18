package ch.magori.blox.core.data

/**
 * What a submitted score did to the record.
 *
 * @property previous the record before the score was submitted.
 * @property isNewRecord whether the score is higher than [previous], and is now the record.
 */
data class RecordResult(val previous: Int, val isNewRecord: Boolean)
