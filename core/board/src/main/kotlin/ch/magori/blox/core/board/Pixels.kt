package ch.magori.blox.core.board

import kotlin.math.roundToInt

/** The nearest whole pixel: cells and panels drawn from it meet without blurred seams. */
fun Float.roundToPixel(): Float = roundToInt().toFloat()
