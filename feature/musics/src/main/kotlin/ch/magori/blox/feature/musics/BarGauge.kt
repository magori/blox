package ch.magori.blox.feature.musics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The gauge of a music [listening] to, under its row: a bar per bar of the music, side by side, those played, the one
 * playing included, in `accent-pressed`, the others in `panel-border`. TalkBack leaves it out: the info line already
 * says the bar played.
 */
@Composable
internal fun BarGauge(
    listening: Listening,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.pill
    Row(
        modifier = modifier
            .testTag(MUSICS_GAUGE_TAG)
            .semantics { hideFromAccessibility() },
        horizontalArrangement = Arrangement.spacedBy(GAUGE_GAP),
    ) {
        gaugeFills(listening).forEach { played ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(GAUGE_HEIGHT)
                    .testTag(MUSICS_GAUGE_BAR_TAG)
                    .background(if (played) palette.accentPressed else palette.panelBorder, shape),
            )
        }
    }
}

/** Each bar of the music [listening] to, played (the one playing included) or not, in their order. */
internal fun gaugeFills(listening: Listening): List<Boolean> = List(listening.bars) { it < listening.bar }

/** The height of a bar of the gauge, and the gap between two bars, as in the mockup. */
private val GAUGE_HEIGHT = 4.dp
private val GAUGE_GAP = 3.dp
