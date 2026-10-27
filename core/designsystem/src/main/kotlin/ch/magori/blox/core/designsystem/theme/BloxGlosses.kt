package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One layer of a [Gloss]: a shadow without blur, inside the shape or around it. */
@Immutable
sealed interface GlossLayer {
    /**
     * A band of [color] inside the shape, [offsetY] high: along the top when it is positive, along the bottom when it
     * is negative. CSS writes it `inset 0 <offsetY> 0 <color>`.
     */
    data class Inner(val offsetY: Dp, val color: Color) : GlossLayer

    /** A ring of [color] around the shape, [width] wide. CSS writes it `0 0 0 <width> <color>`. */
    data class Ring(val width: Dp, val color: Color) : GlossLayer
}

/** The relief of a bubble, its [layers] drawn over its fill in their order: a gloss, never a drop shadow. */
@Immutable
data class Gloss(val layers: List<GlossLayer>) {
    companion object {
        /** No relief: the Original theme, and what is flat in the Colorful theme. */
        val None = Gloss(emptyList())
    }
}

/**
 * A shadow cast under a panel that stands over the screen (the menu « … »): [color], blurred over [blur], [offsetY]
 * lower than the panel. CSS writes it `0 <offsetY> <blur> <color>`.
 */
@Immutable
data class DropShadow(val offsetY: Dp, val blur: Dp, val color: Color) {
    companion object {
        /** No shadow: the Original theme. */
        val None = DropShadow(offsetY = 0.dp, blur = 0.dp, color = Color.Transparent)
    }
}

/** The reliefs and the drop shadow of the interface, the `shadow` tokens of tokens.json (GlossTokensTest). */
@Immutable
data class BloxGlosses(
    /** Every button and key at rest: a light at the top, a thickness at the bottom (`button-gloss`). */
    val button: Gloss,
    /** A button or key held: its thickness is crushed (`button-gloss-pressed`). */
    val buttonPressed: Gloss,
    /** A cell of a piece of 16 dp and more (`cell-gloss`); by day a ring sets the pastels off the white board. */
    val cell: Gloss,
    /** The shadow cast by the menu « … » under its panel (`menu-shadow`): it stands over the screen. */
    val menu: DropShadow,
)

object BloxGlossSets {
    /** The ink of the day, in which the bubbles of the day are shaded. */
    private val INK_OF_DAY = BloxPalettes.ColorfulLight.ink

    /** How thick a button at rest is, crushed when held, and a cell: the height of its shade at the bottom. */
    private val THICKNESS = 3.dp
    private val CRUSHED = 1.dp
    private val CELL_THICKNESS = 2.dp

    /** The menu stands 4 dp over the screen, its shadow blurred over 16 dp. */
    private val MENU_DROP = 4.dp
    private val MENU_BLUR = 16.dp

    /** The Original theme is flat: neither relief nor shadow. */
    val Original = BloxGlosses(
        button = Gloss.None,
        buttonPressed = Gloss.None,
        cell = Gloss.None,
        menu = DropShadow.None,
    )

    /** The Colorful theme by day. */
    val ColorfulLight = BloxGlosses(
        button = Gloss(listOf(light(0.6f), shade(INK_OF_DAY, 0.18f, THICKNESS))),
        buttonPressed = Gloss(listOf(shade(INK_OF_DAY, 0.18f, CRUSHED))),
        cell = Gloss(
            listOf(
                light(0.9f),
                shade(INK_OF_DAY, 0.16f, CELL_THICKNESS),
                GlossLayer.Ring(width = 1.dp, color = INK_OF_DAY.copy(alpha = 0.14f)),
            ),
        ),
        menu = DropShadow(MENU_DROP, MENU_BLUR, INK_OF_DAY.copy(alpha = 0.16f)),
    )

    /** The Colorful theme by night. */
    val ColorfulDark = BloxGlosses(
        button = Gloss(listOf(light(0.25f), shade(Color.Black, 0.45f, THICKNESS))),
        buttonPressed = Gloss(listOf(shade(Color.Black, 0.45f, CRUSHED))),
        cell = Gloss(listOf(light(0.55f), shade(Color.Black, 0.35f, CELL_THICKNESS))),
        menu = DropShadow(MENU_DROP, MENU_BLUR, Color.Black.copy(alpha = 0.5f)),
    )

    /** A white light of [alpha] along the top, 1 dp high. */
    private fun light(alpha: Float) = GlossLayer.Inner(offsetY = 1.dp, color = Color.White.copy(alpha = alpha))

    /** A shade of [color] at [alpha] along the bottom, [height] high. */
    private fun shade(
        color: Color,
        alpha: Float,
        height: Dp,
    ) = GlossLayer.Inner(offsetY = -height, color = color.copy(alpha = alpha))
}
