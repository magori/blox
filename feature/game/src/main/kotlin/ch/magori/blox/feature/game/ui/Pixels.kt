package ch.magori.blox.feature.game.ui

import kotlin.math.roundToInt

/** The nearest whole pixel: cells and panels drawn from it meet without blurred seams. */
internal fun Float.roundToPixel(): Float = roundToInt().toFloat()
