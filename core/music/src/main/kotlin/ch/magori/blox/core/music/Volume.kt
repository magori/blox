package ch.magori.blox.core.music

/**
 * A volume, in [percent] of the full volume: from 0 (silent) to 100 (full), by steps of 10, the steps the
 * player chooses among. Its [gain] is the one place where a volume becomes an amplitude; the players apply it to what
 * they play.
 */
public data class Volume(public val percent: Int) {
    init {
        require(isVolume(percent)) { "A volume is a multiple of 10 from 0 to 100 percent, was $percent" }
    }

    /**
     * The factor of the amplitude of the samples at this volume, from 0 (silent) to 1 (full): the square of the
     * fraction of the full volume. The ear hears loudness on a curve, not linearly: a linear gain would put almost
     * all the change in the first steps, and from 50 % up the slider would seem to do little. The square spreads it:
     * 50 % is a quarter of the amplitude, about 12 dB below the full volume, and 10 %, the lowest step heard, a
     * hundredth, 40 dB below. Unlike a curve in decibels, it reaches 0 at 0 without a special case.
     */
    public val gain: Float get() = (percent / 100f).let { fraction -> fraction * fraction }

    public companion object {
        /** The full volume, 100 percent. */
        public val FULL: Volume = Volume(100)

        /** The silent volume, 0 percent. */
        public val SILENT: Volume = Volume(0)

        /** The volume of [percent], or null if [percent] is none (a value read from outside, a file). */
        public fun ofPercentOrNull(percent: Int): Volume? = if (isVolume(percent)) Volume(percent) else null

        private fun isVolume(percent: Int): Boolean = percent in 0..100 && percent % 10 == 0
    }
}
