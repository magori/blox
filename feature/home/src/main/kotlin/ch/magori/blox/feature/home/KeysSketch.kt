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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The grid of the sketch: 40 × 24 (docs/design-system/pictogrammes.md, `KEYS_SKETCH`). */
private const val SKETCH_WIDTH = 40f
private const val SKETCH_HEIGHT = 24f

/** The arrows: squares of 8, corners of 2, at the height of 10. */
private const val ARROW_SIDE = 8f
private const val ARROW_CORNER = 2f
private const val ARROW_TOP = 10f

/** B and A: discs of radius 5. */
private const val KEY_RADIUS = 5f

/**
 * The sketch of the keys of the game, on its grid of 40 × 24: the left of each square of the [arrows], the centres of
 * [b] and [a].
 */
@Immutable
internal data class KeysSketchShapes(val arrows: List<Float>, val b: Offset, val a: Offset)

/** The sketch of the keys for the right hand, B lower than A, both on the right of the arrows. */
private val RightHanded = KeysSketchShapes(arrows = listOf(0f, 9f, 18f), b = Offset(31f, 17f), a = Offset(35f, 7f))

/** The sketch of the keys for the [leftHanded], turned over as in a mirror: B and A on the left, A outside. */
internal fun keysSketch(leftHanded: Boolean): KeysSketchShapes {
    if (!leftHanded) return RightHanded
    return KeysSketchShapes(
        arrows = RightHanded.arrows.map { SKETCH_WIDTH - ARROW_SIDE - it }.sorted(),
        b = RightHanded.b.copy(x = SKETCH_WIDTH - RightHanded.b.x),
        a = RightHanded.a.copy(x = SKETCH_WIDTH - RightHanded.a.x),
    )
}

/**
 * The colour of A in the sketch, on a card of [palette]: of its accent and its pressed accent, the one that stands out
 * more from the card (by day the pressed one: the accent stands out from it by 2,1 to 1 only).
 */
internal fun sketchAccent(palette: BloxPalette): Color =
    maxOf(palette.accent, palette.accentPressed, compareBy { contrastWith(it, palette.panel) })

/** The contrast ratio of WCAG 2.1 between [first] and [second]. */
private fun contrastWith(
    first: Color,
    second: Color,
): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

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
        shapes.arrows.forEach { left ->
            drawRoundRect(
                color = tint,
                topLeft = Offset(left * unit, ARROW_TOP * unit),
                size = Size(ARROW_SIDE * unit, ARROW_SIDE * unit),
                cornerRadius = CornerRadius(ARROW_CORNER * unit),
            )
        }
        drawCircle(tint, KEY_RADIUS * unit, shapes.b * unit)
        drawCircle(accent, KEY_RADIUS * unit, shapes.a * unit)
    }
}
