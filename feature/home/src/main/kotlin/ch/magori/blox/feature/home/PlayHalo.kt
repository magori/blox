package ch.magori.blox.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * The halo of Jouer (`accent-ring`, components/Button), [elapsed] ticks after the home showed or was last touched:
 * 0 while there is none, otherwise how far its pulse has gone, from just above 0 as it starts to 1 as it ends. Three
 * pulses of a second follow one another after 3 seconds, then one comes every 10 seconds, counted from the third.
 * Pure and counted in ticks; in reduced effects, the screen draws none.
 */
internal fun playHalo(elapsed: Int): Float {
    val sinceFirst = elapsed - FIRST_PULSE
    val intoPulse = when {
        sinceFirst < 0 -> return 0f
        sinceFirst < FIRST_PULSES * PULSE -> sinceFirst % PULSE
        else -> (sinceFirst - (FIRST_PULSES - 1) * PULSE) % PULSE_EVERY
    }
    return if (intoPulse < PULSE) (intoPulse + 1).toFloat() / PULSE else 0f
}

/** A pulse lasts a second. */
private const val PULSE = 60

/** The first pulse comes after 3 seconds. */
private const val FIRST_PULSE = 3 * 60

/** Three pulses come one after another first. */
private const val FIRST_PULSES = 3

/** Then a pulse every 10 seconds, counted from the third. */
private const val PULSE_EVERY = 10 * 60

/** The halo drawn around Jouer: a pill at [topLeft], of [size], its corners of [corner], at [alpha] of `accent-ring`. */
internal data class HaloRing(val topLeft: Offset, val size: Size, val corner: Float, val alpha: Float)

/**
 * The halo around Jouer, of [button] size, [progress] into its pulse: a pill that grows to [spread] pixels around it as
 * it fades out; null without a pulse.
 */
internal fun haloRing(
    progress: Float,
    button: Size,
    spread: Float,
): HaloRing? {
    if (progress <= 0f) return null
    val around = spread * progress
    return HaloRing(
        topLeft = Offset(-around, -around),
        size = Size(button.width + 2 * around, button.height + 2 * around),
        corner = button.height / 2 + around,
        alpha = 1f - progress,
    )
}
