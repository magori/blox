package ch.magori.blox.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The grid of the sketch: 40 × 24 (docs/design-system/pictogrammes.md, `KEYS_SKETCH`). */
private const val SKETCH_WIDTH = 40f
private const val SKETCH_HEIGHT = 24f

/** The arrows: squares of 8, corners of 2. */
private const val ARROW_SIDE = 8f
private const val ARROW_CORNER = 2f

/** B and A: discs of radius 5. */
private const val KEY_RADIUS = 5f

/**
 * The sketch of the keys of the game, on its grid of 40 × 24: the top left corner of each square of the [arrows], the
 * centres of [b] and [a].
 */
@Immutable
internal data class KeysSketchShapes(val arrows: List<Offset>, val b: Offset, val a: Offset)

/**
 * The sketch of the keys for the right hand: ◀ and ▶ at the height of 10, ▼ a third of an arrow lower, as in the game;
 * B lower than A, both on the right of the arrows, B 2 units clear of them.
 */
private val RightHanded = KeysSketchShapes(
    arrows = listOf(Offset(0f, 10f), Offset(9f, 13f), Offset(18f, 10f)),
    b = Offset(33f, 17f),
    a = Offset(35f, 7f),
)

/** The sketch of the keys for the [leftHanded], turned over as in a mirror: B and A on the left, A outside. */
internal fun keysSketch(leftHanded: Boolean): KeysSketchShapes {
    if (!leftHanded) return RightHanded
    return KeysSketchShapes(
        arrows = RightHanded.arrows.map { it.copy(x = SKETCH_WIDTH - ARROW_SIDE - it.x) }.sortedBy { it.x },
        b = RightHanded.b.copy(x = SKETCH_WIDTH - RightHanded.b.x),
        a = RightHanded.a.copy(x = SKETCH_WIDTH - RightHanded.a.x),
    )
}

/**
 * The colour of A in the sketch, on a card of [palette]: its pressed accent, which stands out from the card by 3:1 at
 * least by day and by night (the accent of the day, by 2,1 to 1 only).
 */
internal fun sketchAccent(palette: BloxPalette): Color = palette.accentPressed

/**
 * The sketch of the keys, the pictogram of the setting Gaucher: three arrows and B in [tint], A in its accent, laid out
 * for the [leftHanded] or not. Decorative: the sub-line of the setting says the same.
 */
@Composable
internal fun KeysSketch(
    leftHanded: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val shapes = keysSketch(leftHanded)
    val accent = sketchAccent(BloxTheme.palette)
    Canvas(modifier.size(SKETCH_WIDTH.dp, SKETCH_HEIGHT.dp)) {
        val unit = size.width / SKETCH_WIDTH
        shapes.arrows.forEach { corner ->
            drawRoundRect(
                color = tint,
                topLeft = corner * unit,
                size = Size(ARROW_SIDE * unit, ARROW_SIDE * unit),
                cornerRadius = CornerRadius(ARROW_CORNER * unit),
            )
        }
        drawCircle(tint, KEY_RADIUS * unit, shapes.b * unit)
        drawCircle(accent, KEY_RADIUS * unit, shapes.a * unit)
    }
}
