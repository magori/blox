package ch.magori.blox.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.component.GlyphButton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The gear button at the top right of the home screen (V1), 48 dp, which opens the settings page (V21); TalkBack reads
 * « Paramètres ». A simple drawing for now: the icons of the project come with P5.54.
 */
@Composable
internal fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlyphButton(
        description = stringResource(R.string.feature_home_settings),
        onClick = onClick,
        modifier = modifier,
        glyph = { gear(it) },
    )
}

/** A gear: a ring with [GEAR_TEETH] teeth around it. */
private fun DrawScope.gear(color: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    drawCircle(color, radius = size.width * RING_RADIUS, center = center, style = Stroke(size.width * RING_STROKE))
    repeat(GEAR_TEETH) { tooth ->
        val angle = 2 * PI * tooth / GEAR_TEETH
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        drawLine(
            color,
            start = center + direction * (size.width * TOOTH_FROM),
            end = center + direction * (size.width * TOOTH_TO),
            strokeWidth = size.width * TOOTH_WIDTH,
            cap = StrokeCap.Butt,
        )
    }
}

private const val GEAR_TEETH = 8
private const val RING_RADIUS = 0.24f
private const val RING_STROKE = 0.12f
private const val TOOTH_FROM = 0.28f
private const val TOOTH_TO = 0.44f
private const val TOOTH_WIDTH = 0.14f
