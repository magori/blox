package ch.magori.blox.feature.home

/**
 * The halo of Jouer (`accent-ring`, components/Button), [elapsed] ticks after the home showed or was last touched:
 * 0 while there is none, otherwise how far its pulse has gone, from just above 0 as it starts to 1 as it ends. Three
 * pulses of a second follow one another after 3 seconds, then one comes every 10 seconds. Pure and counted in ticks;
 * in reduced effects, the screen draws none.
 */
internal fun playHalo(elapsed: Int): Float {
    val sinceFirst = elapsed - FIRST_PULSE
    val intoPulse = when {
        sinceFirst < 0 -> return 0f
        sinceFirst < FIRST_PULSES * PULSE -> sinceFirst % PULSE
        else -> sinceFirst % PULSE_EVERY
    }
    return if (intoPulse < PULSE) (intoPulse + 1).toFloat() / PULSE else 0f
}

/** A pulse lasts a second. */
private const val PULSE = 60

/** The first pulse comes after 3 seconds. */
private const val FIRST_PULSE = 3 * 60

/** Three pulses come one after another first. */
private const val FIRST_PULSES = 3

/** Then a pulse every 10 seconds, counted from the first. */
private const val PULSE_EVERY = 10 * 60
