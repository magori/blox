package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A pill button of a screen, 120 × 56, as the sheet shows it. */
private val PillButton = DpSize(120.dp, 56.dp)

/**
 * The reliefs of the theme [kind], on the page and the board of its variant: a button at rest and held, the key A and
 * an arrow at rest, and cells of 22 and 40 dp, the size of the board on a phone and on a tablet.
 */
@Composable
private fun GlossSheet(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        val palette = BloxTheme.palette
        val shapes = BloxTheme.shapes
        val glosses = BloxTheme.glosses
        Column(
            modifier = Modifier.fillMaxSize().background(palette.page).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlossRow("button-gloss") { Buttons(palette.button, palette.accent, glosses.button) }
            GlossRow("button-gloss-pressed") {
                Buttons(palette.buttonPressed, palette.accentPressed, glosses.buttonPressed)
            }
            GlossRow("cell-gloss") {
                Row(
                    modifier = Modifier.background(palette.boardBackground).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(22.dp, 40.dp).forEach { side ->
                        Bubble(DpSize(side, side), shapes.cell, palette.accent, glosses.cell, frame = null)
                    }
                }
            }
        }
    }
}

/** A button of a screen, an arrow and the key A, filled in [fill] and the key A in [accent], with [gloss]. */
@Composable
private fun RowScope.Buttons(
    fill: Color,
    accent: Color,
    gloss: Gloss,
) {
    val palette = BloxTheme.palette
    val shapes = BloxTheme.shapes
    val arrow = DpSize(BloxSizes.keyArrow, BloxSizes.keyArrow)
    val rotation = DpSize(BloxSizes.keyRotate, BloxSizes.keyRotate)
    Bubble(PillButton, shapes.pill, fill, gloss, palette.buttonOutline)
    Bubble(arrow, shapes.md, fill, gloss, palette.buttonOutline)
    Bubble(rotation, shapes.pill, accent, gloss, palette.accentPressed)
}

/** A gloss named [token], and what it is drawn on. */
@Composable
private fun GlossRow(
    token: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = token, color = BloxTheme.palette.label, fontSize = 11.sp, style = BloxTheme.typography.label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

/** A [fill] of [size] in [shape] with its [gloss], inside a frame of 2 dp in [frame] if it has one. */
@Composable
private fun Bubble(
    size: DpSize,
    shape: Shape,
    fill: Color,
    gloss: Gloss,
    frame: Color?,
) {
    val frameWidth = if (frame == null) 0.dp else BloxBorders.frame
    Box(
        modifier = Modifier
            .size(size)
            .background(fill, shape)
            .gloss(gloss, shape, frameWidth)
            .then(if (frame == null) Modifier else Modifier.border(frameWidth, frame, shape)),
    )
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 420)
@Composable
internal fun GlossSheetOriginalPreview() {
    GlossSheet(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 420)
@Composable
internal fun GlossSheetColorfulPreview() {
    GlossSheet(BloxThemeKind.COLORFUL)
}
