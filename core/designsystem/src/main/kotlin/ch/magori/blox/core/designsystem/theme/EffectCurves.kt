package ch.magori.blox.core.designsystem.theme

import kotlin.math.PI
import kotlin.math.sin

/**
 * From 1 at tick 0 down to 0 at the [length]th, and nothing before nor after it: a light that fades (ADR 0024).
 */
fun fadeOut(
    tick: Int,
    length: Int,
): Float = if (tick < 0 || tick >= length) 0f else 1f - tick.toFloat() / length

/**
 * From 0 at tick 0 up to 1 halfway through [length], and back down to 0 at its end, nothing outside it: one swell,
 * never a flash (ADR 0024).
 */
fun swell(
    tick: Int,
    length: Int,
): Float = if (tick < 0 || tick >= length) 0f else sin(PI * tick / length).toFloat()
