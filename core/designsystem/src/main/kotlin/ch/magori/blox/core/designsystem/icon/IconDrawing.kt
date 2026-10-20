package ch.magori.blox.core.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The sizes of the icons and their room, from the mockup (section Icônes). */
object IconSizes {
    /** An icon beside a label: Reprendre, Menu, Rejouer, Au hasard, the items of a menu. */
    val besideLabel = 16.dp

    /** An icon above a label: the toolbar of the editor. */
    val aboveLabel = 14.dp

    /** Between an icon and its label, on a button. */
    val gap = 8.dp

    /** Between an icon and its label, in a menu. */
    val menuGap = 12.dp
}

/**
 * [icon], [size] square, in [tint] (the colour of the content, as a text), drawn as the theme draws its icons: in full
 * pixels in Original, in rounded strokes otherwise. Decorative when a label goes with it, unread by TalkBack; an icon
 * alone has a [contentDescription].
 */
@Composable
fun BloxIcon(
    icon: BloxIcon,
    modifier: Modifier = Modifier,
    size: Dp = IconSizes.besideLabel,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    val described = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }
    when (iconStyleOf(BloxTheme.kind)) {
        IconStyle.PIXELS -> PixelIcon(icon, tint, described.size(size))
        IconStyle.STROKES -> StrokeIcon(icon, tint, described.size(size))
    }
}

/** [icon] in full pixels, each a square of whole device pixels ([pixelRects]). */
@Composable
private fun PixelIcon(
    icon: BloxIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val rects = pixelRects(icon.pixels, size.minDimension.toInt())
            onDrawBehind {
                rects.forEach { rect ->
                    drawRect(
                        tint,
                        topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                        size = Size(rect.width.toFloat(), rect.height.toFloat()),
                    )
                }
            }
        },
    )
}

/** [icon] in rounded strokes ([strokeIcon]). */
@Composable
private fun StrokeIcon(
    icon: BloxIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val vector = remember(icon) { strokeIcon(icon) }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}
