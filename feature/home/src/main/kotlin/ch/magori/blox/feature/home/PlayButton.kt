package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.component.PlayButton as SharedPlayButton

/**
 * The tag of the play button, the same in every language, read by the tests and by the journey of `:benchmark`, which
 * copies it as it is: it does not see this module (`PlayTagTest`).
 */
internal const val PLAY_TAG = "home_play"

/**
 * Jouer of the home ([SharedPlayButton] of the design system), the [reminder] of the settings of the game under it,
 * TalkBack reading [description]. Around it, its [halo] in `accent-ring` ([playHalo]), read while drawing only.
 */
@Composable
internal fun PlayButton(
    reminder: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    halo: () -> Float = { 0f },
) {
    val ring = BloxTheme.palette.accentRing
    Box(modifier = modifier.fillMaxWidth()) {
        // The halo on a layer of its own, behind Jouer: each step of a pulse draws it alone again.
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer()
                .drawBehind { drawHalo(halo(), ring) },
        )
        SharedPlayButton(
            label = stringResource(R.string.feature_home_play),
            reminder = reminder,
            description = description,
            onClick = onClick,
            modifier = Modifier.testTag(PLAY_TAG),
        )
    }
}

/**
 * The halo of Jouer, [progress] into its pulse: a pill of [ring] around the button, which grows to [HALO_SPREAD] as it
 * fades; nothing at 0.
 */
private fun DrawScope.drawHalo(
    progress: Float,
    ring: Color,
) {
    val halo = haloRing(progress, size, HALO_SPREAD.toPx()) ?: return
    drawRoundRect(
        color = ring,
        alpha = halo.alpha,
        topLeft = halo.topLeft,
        size = halo.size,
        cornerRadius = CornerRadius(halo.corner),
    )
}

/** How far the halo of Jouer goes around it at the end of a pulse. */
private val HALO_SPREAD = 12.dp
