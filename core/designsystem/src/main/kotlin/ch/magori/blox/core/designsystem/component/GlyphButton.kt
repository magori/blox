package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes

/** The tag of the icon of a [GlyphButton] of an icon alone, decorative, read by the tests only. */
const val GLYPH_BUTTON_ICON_TAG = "glyph_button_icon"

/** The width of the strokes of a glyph, and the radius of its dots, in parts of its box. */
private const val GLYPH_STROKE = 0.12f
private const val DOT_RADIUS = 0.09f

/**
 * A button of the family Icône that shows a glyph alone, drawn by [glyph] in the ink of the button in a box of
 * [IconSizes.alone], on a disc of 48 dp; TalkBack reads [description]. [emphasis] and [active] as for the button of an
 * icon.
 */
@Composable
fun GlyphButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: ButtonEmphasis = ButtonEmphasis.PLAIN,
    active: Boolean = false,
    glyph: DrawScope.(ink: Color) -> Unit,
) {
    AloneButton(description, onClick, enabled, emphasis, active, modifier) { Glyph(glyph) }
}

/**
 * A button of the family Icône that shows [icon] alone, [IconSizes.alone] square, in the ink of the button, on a disc
 * of 48 dp; TalkBack reads [description], once: the icon itself has none. Selected ([emphasis]), the choice made
 * (Utiliser, En jeu), it is in `accent`; bare (« … », − and + of the tempo, the cross of a message), it has neither
 * fill, nor frame, nor relief until it is pressed; [active], at work (Écouter while it plays), it keeps its pressed
 * fill without going down.
 */
@Composable
fun GlyphButton(
    icon: BloxIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: ButtonEmphasis = ButtonEmphasis.PLAIN,
    active: Boolean = false,
) {
    AloneButton(description, onClick, enabled, emphasis, active, modifier) {
        BloxIcon(icon, Modifier.testTag(GLYPH_BUTTON_ICON_TAG), size = IconSizes.alone)
    }
}

/**
 * A bubble button of the family Icône that shows [content] alone, standing out as [emphasis] says, [active] or not;
 * TalkBack reads [description].
 */
@Composable
private fun AloneButton(
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasis: ButtonEmphasis,
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BubbleButton(
        family = ButtonFamily.ICON,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
        enabled = enabled,
        emphasis = emphasis,
        active = active,
    ) {
        content()
    }
}

/**
 * A bare disc of a glyph alone on the ink of a message (its cross), drawn by [glyph] in the colour of the page until it
 * is pressed; TalkBack reads [description].
 */
@Composable
internal fun GlyphButtonOnInk(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: DrawScope.(ink: Color) -> Unit,
) {
    BareButtonOnInk(
        family = ButtonFamily.ICON,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Glyph(glyph)
    }
}

/** A glyph drawn by [glyph] in the ink of the button that bears it, [IconSizes.alone] square. */
@Composable
private fun Glyph(glyph: DrawScope.(ink: Color) -> Unit) {
    val ink = LocalContentColor.current
    Spacer(modifier = Modifier.size(IconSizes.alone).drawBehind { glyph(ink) })
}

/** The glyphs of the buttons, drawn in the box they are given. */
object Glyphs {
    /** A chevron pointing left: back. */
    fun back(
        scope: DrawScope,
        color: Color,
    ) = with(scope) {
        val path = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.2f)
            lineTo(size.width * 0.3f, size.height * 0.5f)
            lineTo(size.width * 0.62f, size.height * 0.8f)
        }
        drawPath(
            path,
            color,
            style = Stroke(width = size.width * GLYPH_STROKE, cap = StrokeCap.Square, join = StrokeJoin.Miter),
        )
    }

    /** Three dots in a row: « … », more actions. */
    fun more(
        scope: DrawScope,
        color: Color,
    ) = with(scope) {
        listOf(0.2f, 0.5f, 0.8f).forEach { x ->
            drawCircle(color, radius = size.width * DOT_RADIUS, center = Offset(size.width * x, size.height / 2))
        }
    }

    /** A dash: one step down. */
    fun minus(
        scope: DrawScope,
        color: Color,
    ) = with(scope) {
        drawLine(
            color,
            Offset(size.width * 0.2f, size.height / 2),
            Offset(size.width * 0.8f, size.height / 2),
            size.width * GLYPH_STROKE,
        )
    }

    /** A dash crossed upright: one step up. */
    fun plus(
        scope: DrawScope,
        color: Color,
    ) = with(scope) {
        minus(this, color)
        drawLine(
            color,
            Offset(size.width / 2, size.height * 0.2f),
            Offset(size.width / 2, size.height * 0.8f),
            size.width * GLYPH_STROKE,
        )
    }

    /** A cross: close. */
    fun close(
        scope: DrawScope,
        color: Color,
    ) = with(scope) {
        val stroke = size.width * GLYPH_STROKE
        drawLine(
            color,
            Offset(size.width * 0.25f, size.height * 0.25f),
            Offset(size.width * 0.75f, size.height * 0.75f),
            stroke,
        )
        drawLine(
            color,
            Offset(size.width * 0.75f, size.height * 0.25f),
            Offset(size.width * 0.25f, size.height * 0.75f),
            stroke,
        )
    }
}
