package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The tag of the icon of a [GlyphButton] of an icon alone, decorative, read by the tests only. */
const val GLYPH_BUTTON_ICON_TAG = "glyph_button_icon"

/** The width of the strokes of a glyph, and the radius of its dots, in parts of its box. */
private const val GLYPH_STROKE = 0.12f
private const val DOT_RADIUS = 0.09f

/**
 * A button that shows a glyph alone, drawn by [glyph] in [ink] in a box of [IconSizes.alone], on a touch target of
 * 48 dp, on [background] (transparent unless told otherwise); TalkBack reads [description].
 */
@Composable
fun GlyphButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = BloxTheme.palette.ink,
    background: Color = Color.Transparent,
    glyph: DrawScope.(ink: Color) -> Unit,
) {
    AloneButton(description, onClick, background, modifier) {
        Spacer(modifier = Modifier.size(IconSizes.alone).drawBehind { glyph(ink) })
    }
}

/**
 * A button that shows [icon] alone, [IconSizes.alone] square, in [ink], on a touch target of 48 dp, on [background]
 * (transparent unless told otherwise); TalkBack reads [description], once: the icon itself has none.
 */
@Composable
fun GlyphButton(
    icon: BloxIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = BloxTheme.palette.ink,
    background: Color = Color.Transparent,
) {
    AloneButton(description, onClick, background, modifier) {
        BloxIcon(icon, Modifier.testTag(GLYPH_BUTTON_ICON_TAG), size = IconSizes.alone, tint = ink)
    }
}

/** A button of 48 dp at least, on [background], that shows [content] alone; TalkBack reads [description]. */
@Composable
private fun AloneButton(
    description: String,
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = BloxTheme.shapes.md
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = BloxSizes.minTouchTarget, minHeight = BloxSizes.minTouchTarget)
            .clip(shape)
            .background(background, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
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
