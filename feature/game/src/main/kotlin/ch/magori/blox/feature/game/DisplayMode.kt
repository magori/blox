package ch.magori.blox.feature.game

import kotlin.math.abs
import kotlin.math.roundToInt

/** A mode of the display, as Android lists them: its [id], its resolution, its refresh rate in hertz. */
internal data class DisplayMode(val id: Int, val width: Int, val height: Int, val refreshRate: Float)

/**
 * The mode of the display the game asks for while it is played, among the [supported] ones, at the
 * resolution of the [current] mode; null when none suits it, and the display is then left as it is.
 *
 * The simulation runs at 60 ticks a second (ADR 0006): at 90 Hz, frames carry one tick or none in turn,
 * and the piece falls unevenly; at 120 Hz, every other frame draws nothing new. The game therefore asks
 * for the slowest mode that gives each tick the same whole number of frames: 60 Hz, or failing that
 * 120 Hz. A rate reported a hair off (59.94 Hz) counts as the multiple it rounds to, after the rate that
 * hits it exactly.
 */
internal fun gameDisplayMode(
    current: DisplayMode,
    supported: List<DisplayMode>,
): DisplayMode? = supported
    .filter { it.width == current.width && it.height == current.height }
    .filter { it.gapToWholeFrames() < REFRESH_RATE_TOLERANCE }
    .minWithOrNull(compareBy<DisplayMode> { it.framesPerTick() }.thenBy { it.gapToWholeFrames() })

/** The largest gap, in hertz, between a reported refresh rate and the multiple of 60 Hz it stands for. */
private const val REFRESH_RATE_TOLERANCE = 0.5f

/** The whole number of frames this mode gives each tick, its rate rounded to a multiple of 60 Hz. */
private fun DisplayMode.framesPerTick(): Int = (refreshRate / TICKS_PER_SECOND).roundToInt()

/** The gap, in hertz, between the rate of this mode and the multiple of 60 Hz it rounds to. */
private fun DisplayMode.gapToWholeFrames(): Float = abs(refreshRate - framesPerTick() * TICKS_PER_SECOND)
