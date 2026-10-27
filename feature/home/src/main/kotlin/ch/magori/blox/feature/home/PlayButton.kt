package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The tag of the play button, the same in every language, read by the tests and by the journey of `:benchmark`, which
 * copies it as it is: it does not see this module (`PlayTagTest`).
 */
internal const val PLAY_TAG = "home_play"

/**
 * Jouer (Principal XL), as wide as the column: « Jouer » above the [reminder] of the settings of the game (« Classique
 * · Niveau 05 · Korobeïniki »), cut by an ellipsis when too long, and a disc in `accent-ink` bearing the triangle in
 * `accent`, decorative. TalkBack reads [description], Jouer and the whole reminder in words. Around it, its [halo] in
 * `accent-ring` ([playHalo]), read while drawing only.
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
        BubbleButton(
            family = ButtonFamily.PRIMARY_XL,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PLAY_TAG)
                .semantics { contentDescription = description },
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SingleLineText(text = stringResource(R.string.feature_home_play), style = LocalTextStyle.current)
                Text(
                    text = reminder,
                    style = MaterialTheme.typography.labelMedium.merge(BloxTheme.typography.small)
                        .copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlayDisc()
        }
    }
}

/** The disc of Jouer, in `accent-ink`, and its triangle in `accent`; square in Original, as every pill. */
@Composable
private fun PlayDisc() {
    val palette = BloxTheme.palette
    Box(
        modifier = Modifier.size(PLAY_DISC).background(palette.accentInk, BloxTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        BloxIcon(BloxIcon.PLAY, Modifier.testTag(HOME_ICON_TAG), tint = palette.accent)
    }
}

/** The disc of Jouer (docs/design-system/README.md, Boutons). */
private val PLAY_DISC = 52.dp

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
