package ch.magori.blox.core.board.logo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.mascot.Mascot
import ch.magori.blox.core.board.mascot.MascotCharacter
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant

/**
 * The logo BloX v2 of the Colorful theme (docs/design-system/pictogrammes.md, Logo BloX v2), its letters [height]
 * high: « Bl » and « X » in Bagel Fat One (`logo`), in the iridescent gradients of [logoLook] over their shadows, and
 * between them the Locksmith mascot for the « o », standing on their baseline. Its size does not follow the font of the
 * system: it is a picture. Decorative: the caller names it to TalkBack, « BloX », the title of its screen.
 */
@Composable
fun BloxLogo(
    modifier: Modifier = Modifier,
    height: Dp = LOGO_HEIGHT,
) {
    val look = logoLook(night = BloxTheme.variant == BloxThemeVariant.COLORFUL_DARK)
    // k, the eightieth of the height of the letters, the unit of the measures of the logo.
    val k = height / UNITS
    val style = BloxTheme.typography.logo.copy(fontSize = with(LocalDensity.current) { height.toSp() })
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(k * GAP),
    ) {
        LogoLetters("Bl", style, look.letters, look.lettersShadows, k, Modifier.alignByBaseline())
        Mascot(
            character = MascotCharacter.LOCKSMITH,
            cellSize = k * MASCOT_CELL,
            modifier = Modifier
                .alignBy { it.measuredHeight }
                .padding(horizontal = k * MASCOT_SIDES)
                .offset(y = -k * MASCOT_RAISE),
        )
        LogoLetters("X", style, look.x, look.xShadows, k, Modifier.alignByBaseline())
    }
}

/** The [text] of the logo in the gradient of [colors], at 135°, over its [shadows], counted in [k]. */
@Composable
private fun LogoLetters(
    text: String,
    style: TextStyle,
    colors: List<Color>,
    shadows: List<LogoShadow>,
    k: Dp,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val laidOut = remember(text, style, measurer) { measurer.measure(text, style) }
    val brush = remember(colors) { gradientOf(colors) }
    BasicText(
        text = text,
        style = style.copy(brush = brush),
        modifier = modifier.drawBehind {
            val unit = k.toPx()
            shadows.forEach { shadow ->
                val drawn = shadow.drawnAt(unit)
                translate(top = drawn.lower) { drawText(laidOut, color = drawn.letters, shadow = drawn.blur) }
            }
        },
    )
}

/** The gradient of the letters, from the top left to the bottom right: the middle stop of three at 60 %. */
private fun gradientOf(colors: List<Color>): Brush {
    val stops = if (colors.size == THREE_STOPS) {
        arrayOf(0f to colors[0], MIDDLE_STOP to colors[1], 1f to colors[2])
    } else {
        colors.mapIndexed { index, color -> index.toFloat() / (colors.size - 1) to color }.toTypedArray()
    }
    return Brush.linearGradient(*stops, start = Offset.Zero, end = Offset.Infinite)
}

/** The height of the letters on the home of a phone held upright. */
private val LOGO_HEIGHT = 56.dp
private const val UNITS = 80f
private const val GAP = 2f
private const val MASCOT_CELL = 25f
private const val MASCOT_SIDES = 4f
private const val MASCOT_RAISE = 6f
private const val THREE_STOPS = 3
private const val MIDDLE_STOP = 0.6f

@Composable
private fun LogoPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Column(modifier = Modifier.background(BloxTheme.palette.page).padding(24.dp)) {
            BloxLogo()
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render this preview, by day and by night.

@Preview
@Composable
internal fun BloxLogoPreview() {
    LogoPreview()
}
