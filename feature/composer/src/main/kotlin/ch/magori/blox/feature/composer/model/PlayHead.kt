package ch.magori.blox.feature.composer.model

/** Ticks per second: 1 tick = 1 frame = 1/60 s (ADR 0006). */
internal const val TICKS_PER_SECOND: Int = 60

/**
 * Where the music listened to stands: the [cell] being played. It counts in ticks, never in milliseconds (ADR 0006):
 * its [progress] is the sum, over the ticks played, of the tempo they were played at, in beats per minute, so that a
 * change of tempo moves it on at the new tempo from where it stands, with integers and no drift. It loops with the
 * music, after the eighth bar.
 */
internal data class PlayHead(private val progress: Long) {
    /** The cell being played. */
    val cell: Cell
        get() {
            val index = (progress / PROGRESS_PER_CELL).toInt()
            return Cell(bar = index / CELLS_PER_BAR, eighth = index % CELLS_PER_BAR)
        }

    /** The play head [ticks] later, played at [tempo]. */
    fun advance(
        ticks: Int,
        tempo: Tempo,
    ): PlayHead = PlayHead((progress + ticks.toLong() * tempo.bpm) % PROGRESS_PER_LOOP)

    companion object {
        /** The start of the music. */
        val START: PlayHead = PlayHead(0)

        /** A cell, an eighth note, lasts half a beat: at 1 beat per minute, 30 seconds of ticks. */
        private const val PROGRESS_PER_CELL = 60L * TICKS_PER_SECOND / 2

        /** The music loops after its eight bars. */
        private const val PROGRESS_PER_LOOP = PROGRESS_PER_CELL * CELLS_PER_BAR * BARS
    }
}
