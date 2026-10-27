package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The parts of the home (V27), each drawn on its own, so that a layout only places them: the [bar] (Changer de jeu,
 * the record, Paramètres), the [hero] (the logo, then the preview), [play], the card of the [settings] and the
 * [tiles].
 */
internal class HomeParts(
    val bar: @Composable () -> Unit,
    val hero: @Composable () -> Unit,
    val play: @Composable () -> Unit,
    val settings: @Composable () -> Unit,
    val tiles: @Composable () -> Unit,
)

/**
 * The home laid out for a phone held upright (docs/design-system/README.md, Mise en page): the [parts] in one column,
 * from the top, 16 dp from the edges and from one another, which scrolls on a small phone. The phone on its side and
 * the tablets (P5.84) will place the same parts otherwise, chosen by the room available.
 */
@Composable
internal fun HomeColumn(
    parts: HomeParts,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(BloxSpaces.space4),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        parts.bar()
        parts.hero()
        parts.play()
        parts.settings()
        parts.tiles()
    }
}
