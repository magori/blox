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
 * [IconSizes.alone], on a disc of 48 dp; TalkBack reads [description]. [selected], [active] and [bare] as for the
 * button of an icon.
 */
@Composable
fun GlyphButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    active: Boolean = false,
    bare: Boolean = false,
    glyph: DrawScope.(ink: Color) -> Unit,
) {
    AloneButton(description, onClick, enabled, selected, active, bare, modifier) {
        val ink = LocalContentColor.current
        Spacer(modifier = Modifier.size(IconSizes.alone).drawBehind { glyph(ink) })
    }
}

/**
 * A button of the family Icône that shows [icon] alone, [IconSizes.alone] square, in the ink of the button, on a disc
 * of 48 dp; TalkBack reads [description], once: the icon itself has none. [selected], the choice made (Utiliser, En
 * jeu), it is filled and framed in `accent`; [active], at work (Écouter while it plays), it keeps its pressed fill
 * without going down; [bare] (« … », − and + of the tempo, the cross of a message), it has neither fill, nor frame,
 * nor relief until it is pressed.
 */
@Composable
fun GlyphButton(
    icon: BloxIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    active: Boolean = false,
    bare: Boolean = false,
) {
    AloneButton(description, onClick, enabled, selected, active, bare, modifier) {
        BloxIcon(icon, Modifier.testTag(GLYPH_BUTTON_ICON_TAG), size = IconSizes.alone)
    }
}

/**
 * A bubble button of the family Icône that shows [content] alone, [selected], [active] and [bare] or not; TalkBack
 * reads [description].
 */
@Composable
private fun AloneButton(
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    selected: Boolean,
    active: Boolean,
    bare: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BubbleButton(
        family = ButtonFamily.ICON,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
        enabled = enabled,
        selected = selected,
        active = active,
        bare = bare,
    ) {
        content()
    }
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
