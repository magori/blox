package ch.magori.blox.core.designsystem.frame

/**
 * Where a looped music listened to stands: the [eighth] note being played, counted from 0 at the start of a loop of
 * [eighthsPerLoop] eighth notes. Shared by the screens that show a music as it plays: the music editor (8 bars of 8
 * eighth notes) and the musical garden (its 16 columns, 2 bars).
 *
 * It counts in ticks, never in milliseconds (ADR 0006): its progress is the sum, over the ticks played, of the tempo
 * they were played at, in beats per minute, so that a change of tempo moves it on at the new tempo from where it
 * stands, with integers and no drift. An eighth note lasts half a beat. It loops with the music, after its last eighth
 * note. The ticks come from the frames of the screen, through [FrameTicks].
 *
 * Only [eighth] (and [eighthsPerLoop]) can be observed: two play heads on the same eighth note are not equal, as
 * there is no value equality, and they may stand at different points of that eighth note.
 *
 * @throws IllegalArgumentException if [eighthsPerLoop] is zero or negative.
 */
class LoopPlayHead private constructor(val eighthsPerLoop: Int, private val progress: Long) {
    init {
        require(eighthsPerLoop > 0) { "eighthsPerLoop must be positive, was $eighthsPerLoop" }
    }

    /** The eighth note being played, from 0 to [eighthsPerLoop] - 1. */
    val eighth: Int get() = (progress / PROGRESS_PER_EIGHTH).toInt()

    /**
     * The play head [ticks] later, played at [beatsPerMinute].
     *
     * @throws IllegalArgumentException if [ticks] is negative or [beatsPerMinute] is zero or negative.
     */
    fun advance(
        ticks: Int,
        beatsPerMinute: Int,
    ): LoopPlayHead {
        require(ticks >= 0) { "ticks must not be negative, was $ticks" }
        require(beatsPerMinute > 0) { "beatsPerMinute must be positive, was $beatsPerMinute" }
        return LoopPlayHead(
            eighthsPerLoop,
            (progress + ticks.toLong() * beatsPerMinute) % (PROGRESS_PER_EIGHTH * eighthsPerLoop),
        )
    }

    companion object {
        /** An eighth note lasts half a beat: at 1 beat per minute, 30 seconds of ticks. */
        private const val PROGRESS_PER_EIGHTH = 60L * TICKS_PER_SECOND / 2

        /** The start of a loop of [eighthsPerLoop] eighth notes. */
        fun start(eighthsPerLoop: Int): LoopPlayHead = LoopPlayHead(eighthsPerLoop, progress = 0)
    }
}
