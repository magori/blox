package ch.magori.blox.feature.cartridges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/**
 * The choice of the game (V26), the first screen of the application: its title, then [cartridges], one under the
 * other, each read by TalkBack as a button of its name. Tapping one hands it to [onOpen], which opens its home.
 */
@Composable
internal fun CartridgesScreen(
    cartridges: List<Cartridge>,
    onOpen: (Cartridge) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(BloxSpaces.space6),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space6, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.feature_cartridges_title),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.headlineMedium.merge(BloxTheme.typography.screenTitle),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        // Taps count only while this screen is resumed: once a home is on its way, a second tap does nothing.
        cartridges.forEach { cartridge ->
            CartridgeCard(cartridge = cartridge, onClick = dropUnlessResumed { onOpen(cartridge) })
        }
    }
}

/**
 * A cartridge: a panel at most [CARTRIDGE_WIDTH] wide, framed by `panel-border`, three grips at its top, and its
 * sticker, in `accent`, which bears its name. One button for TalkBack, read by its name.
 */
@Composable
private fun CartridgeCard(
    cartridge: Cartridge,
    onClick: () -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.window
    Column(
        modifier = Modifier
            .testTag(cartridge.tag)
            .widthIn(max = CARTRIDGE_WIDTH)
            .fillMaxWidth()
            .clip(shape)
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(BloxSpaces.space4),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Grips()
        val nameStyle = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.playLabel)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.accent, BloxTheme.shapes.md)
                .padding(horizontal = BloxSpaces.space4, vertical = BloxSpaces.space6),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(cartridge.label),
                color = palette.accentInk,
                style = nameStyle,
                textAlign = TextAlign.Center,
                // On one line, whole: in a large font, the name shrinks rather than being cut inside a word.
                maxLines = 1,
                softWrap = false,
                autoSize = shrinkToFit(nameStyle.fontSize),
            )
        }
    }
}

/** The grips at the top of a cartridge, where a hand would hold it: decorative. */
@Composable
private fun Grips() {
    Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
        repeat(GRIPS) {
            Box(
                Modifier
                    .size(width = GRIP_WIDTH, height = GRIP_HEIGHT)
                    .background(BloxTheme.palette.panelBorder, BloxTheme.shapes.pill),
            )
        }
    }
}

/** The widest a cartridge stands, on a tablet as on a phone. */
private val CARTRIDGE_WIDTH = 240.dp

private const val GRIPS = 3
private val GRIP_WIDTH = 32.dp
private val GRIP_HEIGHT = 4.dp

@Composable
private fun CartridgesPreview(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        CartridgesScreen(cartridges = Cartridge.entries, onOpen = {})
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun CartridgesOriginalPreview() {
    CartridgesPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun CartridgesColorfulPreview() {
    CartridgesPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun CartridgesOriginalLargeFontPreview() {
    CartridgesPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun CartridgesColorfulLargeFontPreview() {
    CartridgesPreview(BloxThemeKind.COLORFUL)
}
