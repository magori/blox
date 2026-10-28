package ch.magori.blox.core.board.mascot

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import kotlin.math.max

/**
 * The mascot [character] (docs/design-system/pictogrammes.md, Mascotte), in cells of [cellSize], awake with its face
 * and its accessory, or [asleep], its eyes shut, two « z » above it. Drawn for the Colorful theme, by day or by night as
 * the theme goes; it measures its body, its accessory and its « z » drawn over its top right corner, beyond it.
 * Decorative: the caller names it to TalkBack when it is not beside a label.
 */
@Composable
fun Mascot(
    character: MascotCharacter,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    asleep: Boolean = false,
) {
    val night = BloxTheme.variant == BloxThemeVariant.COLORFUL_DARK
    val look = mascotLook(character, night, asleep)
    val ink = BloxTheme.palette.ink
    val cells = character.cells
    val columns = cells.maxOf { it.column } + 1
    val rows = cells.maxOf { it.row } + 1
    Spacer(
        modifier = modifier
            .size(cellSize * columns, cellSize * rows)
            .drawWithCache { drawMascot(character, look, ink, asleep) },
    )
}

/** Builds the drawing of [character] once per size: its cells, its face, its accessory or its « z ». */
private fun CacheDrawScope.drawMascot(
    character: MascotCharacter,
    look: MascotLook,
    ink: Color,
    asleep: Boolean,
): DrawResult {
    val cells = character.cells
    val columns = cells.maxOf { it.column } + 1
    val c = size.width / columns
    val stroke = max(MIN_STROKE.dp.toPx(), STROKE * c)
    val margin = MARGIN.dp.toPx()
    // Each cell lit from its own top, as a cell of a piece.
    val bubbles = cells.map { cell ->
        val top = cell.row * c
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = cell.column * c + margin,
                    top = top + margin,
                    right = (cell.column + 1) * c - margin,
                    bottom = top + c - margin,
                    cornerRadius = CornerRadius(look.corner * c),
                ),
            )
        }
        path to Brush.verticalGradient(0f to look.highlight, HIGHLIGHT_END to look.fill, startY = top, endY = top + c)
    }
    val androidBody = Path().apply { bubbles.forEach { (path, _) -> addPath(path) } }.asAndroidPath()
    val shadow = look.shadow
    val shadowPaint = shadow?.let {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = it.color.toArgb()
            maskFilter = BlurMaskFilter(it.blur * c, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val face = faceCentre(character).let { Offset(it.x * c, it.y * c) }
    return onDrawBehind {
        if (shadow != null && shadowPaint != null) {
            translate(top = shadow.drop * c) { drawIntoCanvas { it.nativeCanvas.drawPath(androidBody, shadowPaint) } }
        }
        bubbles.forEach { (path, brush) -> drawPath(path, brush) }
        if (asleep) {
            drawAsleepFace(face, c, stroke, look.face)
        } else {
            drawAwakeFace(face, c, stroke, look.face)
            look.accessory?.let { drawAccessory(character, columns * c, c, it, ink) }
        }
    }
}

/**
 * The centre of the face, in cells: on the bar of the T, in the middle of the O, on the foot of the L, on the top bar of
 * the Z.
 */
private fun faceCentre(character: MascotCharacter): Offset = when (character) {
    MascotCharacter.CLASSIC -> Offset(1.5f, 0.55f)
    MascotCharacter.LOCKSMITH -> Offset(1f, 0.9f)
    MascotCharacter.GARDEN -> Offset(1f, 2.5f)
    MascotCharacter.DAILY -> Offset(1f, 0.55f)
}

private fun DrawScope.drawAwakeFace(
    centre: Offset,
    c: Float,
    stroke: Float,
    ink: Color,
) {
    for (side in listOf(-1f, 1f)) {
        val eye = centre + Offset(side * EYE_APART * c, -EYE_RAISE * c)
        drawCircle(Color.White, radius = EYE * c / 2, center = eye)
        val pupil = eye + Offset(-side * PUPIL_INWARD * c, PUPIL_DOWN * c)
        drawCircle(ink, radius = EYE * c / 4, center = pupil)
        val cheek = centre + Offset(side * CHEEK_APART * c - CHEEK_WIDTH * c / 2, CHEEK_DOWN * c - CHEEK_HEIGHT * c / 2)
        drawOval(CHEEK, topLeft = cheek, size = Size(CHEEK_WIDTH * c, CHEEK_HEIGHT * c))
    }
    val mouthTop = centre.y + MOUTH_DOWN * c - MOUTH_HEIGHT * c
    drawArc(
        color = ink,
        startAngle = 0f,
        sweepAngle = HALF_TURN,
        useCenter = false,
        topLeft = Offset(centre.x - MOUTH_WIDTH * c / 2, mouthTop),
        size = Size(MOUTH_WIDTH * c, 2 * MOUTH_HEIGHT * c),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawAsleepFace(
    centre: Offset,
    c: Float,
    stroke: Float,
    ink: Color,
) {
    for (side in listOf(-1f, 1f)) {
        val eye = centre + Offset(side * EYE_APART * c, -SLEEP_EYE_RAISE * c)
        drawLine(ink, eye - Offset(EYE * c / 2, 0f), eye + Offset(EYE * c / 2, 0f), stroke, StrokeCap.Round)
    }
    val mouth = centre + Offset(0f, MOUTH_DOWN * c)
    drawLine(
        ink,
        mouth - Offset(SLEEP_MOUTH * c / 2, 0f),
        mouth + Offset(SLEEP_MOUTH * c / 2, 0f),
        stroke,
        StrokeCap.Round,
    )
    drawZ(centre + Offset(BIG_Z_AT.x * c, BIG_Z_AT.y * c), BIG_Z * c, stroke, ink.copy(alpha = BIG_Z_ALPHA))
    drawZ(centre + Offset(SMALL_Z_AT.x * c, SMALL_Z_AT.y * c), SMALL_Z * c, stroke, ink.copy(alpha = SMALL_Z_ALPHA))
}

/** A « z » of [side] whose top left corner is at [at]: its top, its slant, its bottom. */
private fun DrawScope.drawZ(
    at: Offset,
    side: Float,
    stroke: Float,
    color: Color,
) {
    val path = Path().apply {
        moveTo(at.x, at.y)
        lineTo(at.x + side, at.y)
        lineTo(at.x, at.y + side)
        lineTo(at.x + side, at.y + side)
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * The crown of the T, the key of the O, the leaf of the L or the sprout of the Z, in a box of 0,95 cell over the top
 * right corner, turned by 14°.
 */
private fun DrawScope.drawAccessory(
    character: MascotCharacter,
    width: Float,
    c: Float,
    fill: Color,
    ink: Color,
) {
    val box = ACCESSORY * c
    val (outline, extra) = when (character) {
        MascotCharacter.CLASSIC -> CROWN to null
        MascotCharacter.LOCKSMITH -> KEY_RING to KEY_BIT
        MascotCharacter.GARDEN -> LEAF to LEAF_VEIN
        MascotCharacter.DAILY -> SPROUT to SPROUT_STEM
    }
    translate(left = width - ACCESSORY_LEFT * box, top = -ACCESSORY_UP * box) {
        rotate(ACCESSORY_TURN, pivot = Offset(box / 2, box / 2)) {
            scale(box / ICON_GRID, pivot = Offset.Zero) {
                val line = Stroke(ACCESSORY_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(outline, fill)
                drawPath(outline, ink, style = line)
                extra?.let { drawPath(it, ink, style = line) }
            }
        }
    }
}

private fun pathOf(data: String): Path = PathParser().parsePathString(data).toPath()

private val CROWN = pathOf("M3 18 L4.5 7.5 L9 11.5 L12 5 L15 11.5 L19.5 7.5 L21 18 Z")
private val KEY_RING = pathOf("M3.5 8 a4.5 4.5 0 1 0 9 0 a4.5 4.5 0 1 0 -9 0 Z")
private val KEY_BIT = pathOf("M11.2 11.2 L20 20 M15.5 15.5 L18 13 M18 18 L20.5 15.5")
private val SPROUT = pathOf("M12 13 C12 8 8 6 4 6 C4 10 7 13 12 13 Z M12 11 C12 6 16 4 20 4 C20 8 17 11 12 11 Z")
private val SPROUT_STEM = pathOf("M12 21 V11")
private val LEAF = pathOf("M4 20 C4 10 10 4 20 4 C20 14 14 20 4 20 Z")
private val LEAF_VEIN = pathOf("M4 20 L13 11")

/** The grid of the accessories, as of the pictograms. */
private const val ICON_GRID = 24f
private const val ACCESSORY_STROKE = 1.8f
private const val ACCESSORY = 0.95f
private const val ACCESSORY_LEFT = 0.55f
private const val ACCESSORY_UP = 0.6f
private const val ACCESSORY_TURN = 14f

/** The 1 px between two cells, as on the board. */
private const val MARGIN = 1f

/** Where the lighting of a cell reaches its colour, as on the board: 55 % of its height. */
private const val HIGHLIGHT_END = 0.55f

/** The strokes of the face: 0,06 cell, 2 dp at least. */
private const val STROKE = 0.06f
private const val MIN_STROKE = 2f

private const val EYE = 0.3f
private const val EYE_APART = 0.26f
private const val EYE_RAISE = 0.08f
private const val PUPIL_DOWN = 0.03f
private const val PUPIL_INWARD = 0.02f
private const val MOUTH_WIDTH = 0.3f
private const val MOUTH_HEIGHT = 0.14f
private const val MOUTH_DOWN = 0.12f
private const val HALF_TURN = 180f

/** rgba(255,110,150,0.45). */
private val CHEEK = Color(red = 255, green = 110, blue = 150).copy(alpha = 0.45f)
private const val CHEEK_WIDTH = 0.18f
private const val CHEEK_HEIGHT = 0.11f
private const val CHEEK_APART = 0.46f
private const val CHEEK_DOWN = 0.1f

private const val SLEEP_EYE_RAISE = 0.04f
private const val SLEEP_MOUTH = 0.16f
private const val BIG_Z = 0.42f
private val BIG_Z_AT = Offset(0.4f, -0.95f)
private const val BIG_Z_ALPHA = 0.75f
private const val SMALL_Z = 0.3f
private val SMALL_Z_AT = Offset(0.72f, -1.25f)
private const val SMALL_Z_ALPHA = 0.55f

/**
 * The four mascots, awake, then the Classic one asleep, on the page: on two rows, that none be squeezed on a narrow
 * preview.
 */
@Composable
private fun MascotsPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Column(
            modifier = Modifier.background(BloxTheme.palette.page).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.Bottom) {
                Mascot(MascotCharacter.CLASSIC, cellSize = 38.dp)
                Mascot(MascotCharacter.LOCKSMITH, cellSize = 25.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.Bottom) {
                Mascot(MascotCharacter.GARDEN, cellSize = 25.dp)
                Mascot(MascotCharacter.DAILY, cellSize = 25.dp)
                Mascot(MascotCharacter.CLASSIC, cellSize = 22.dp, asleep = true)
            }
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render this preview, by day and by night.

@Preview
@Composable
internal fun MascotColorfulPreview() {
    MascotsPreview()
}
