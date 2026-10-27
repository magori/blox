package ch.magori.blox.core.board.logo

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.IrisColors

/**
 * How the logo BloX v2 is drawn (docs/design-system/pictogrammes.md, Logo BloX v2): « Bl » in the gradient of [letters]
 * (its middle stop at 60 %), « X » in that of [x], each over its shadows, drawn in their order, the farthest first.
 */
@Immutable
internal data class LogoLook(
    val letters: List<Color>,
    val x: List<Color>,
    val lettersShadows: List<LogoShadow>,
    val xShadows: List<LogoShadow>,
)

/**
 * A shadow of the letters, of [color], [drop] lower and blurred over [blur], counted in `k`, the eightieth of the height
 * of the letters.
 */
internal data class LogoShadow(val color: Color, val drop: Float, val blur: Float)

/** The logo of the Colorful theme, by day or at [night]: the `iris` stops of its palette, its shadows. */
internal fun logoLook(night: Boolean): LogoLook = if (night) NIGHT else DAY

private fun IrisColors.stops() = listOf(start, middle, end)

/** By day, three drop shadows under every letter: a soft one, the lilac of `accent`, a white rim. */
private val DAY_SHADOWS = listOf(
    LogoShadow(Color(red = 90, green = 80, blue = 160).copy(alpha = 0.3f), drop = 12f, blur = 14f),
    LogoShadow(BloxPalettes.ColorfulLight.accent, drop = 5f, blur = 0f),
    LogoShadow(Color.White, drop = 2f, blur = 0f),
)

private val DAY = BloxPalettes.ColorfulLight.iris.let { iris ->
    LogoLook(
        letters = iris.stops(),
        x = listOf(iris.end, iris.start),
        lettersShadows = DAY_SHADOWS,
        xShadows = DAY_SHADOWS,
    )
}

/** By night, a dark shadow, `panel-border`, under a halo of the colour the letters start or end with. */
private val NIGHT_SHADOW = LogoShadow(BloxPalettes.ColorfulDark.panelBorder, drop = 4f, blur = 0f)
private const val NIGHT_HALO_BLUR = 18f
private const val LETTERS_HALO_ALPHA = 0.45f
private const val X_HALO_ALPHA = 0.5f

/** The pink the X ends with at night: that of the Y2K board, not a token, kept in the logo alone. */
private val NIGHT_PINK = Color(0xFFFF6FB5)

private val NIGHT = BloxPalettes.ColorfulDark.iris.let { iris ->
    LogoLook(
        letters = iris.stops(),
        x = listOf(iris.end, NIGHT_PINK),
        lettersShadows = listOf(
            LogoShadow(iris.start.copy(alpha = LETTERS_HALO_ALPHA), drop = 0f, blur = NIGHT_HALO_BLUR),
            NIGHT_SHADOW,
        ),
        xShadows = listOf(
            LogoShadow(iris.end.copy(alpha = X_HALO_ALPHA), drop = 0f, blur = NIGHT_HALO_BLUR),
            NIGHT_SHADOW,
        ),
    )
}
