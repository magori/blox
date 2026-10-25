package ch.magori.blox.core.music

/**
 * A volume, in [percent] of the full volume: from 0 (silent) to 100 (full), by steps of 10, the steps the
 * player chooses among. The player applies it to what it plays; the volume itself knows nothing of sound.
 */
public data class Volume(public val percent: Int) {
    init {
        require(isVolume(percent)) { "A volume is a multiple of 10 from 0 to 100 percent, was $percent" }
    }

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
