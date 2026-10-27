package ch.magori.blox.feature.cartridges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.board.logo.BloxLogo
import ch.magori.blox.core.board.mascot.Mascot
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.holdSideBySide
import ch.magori.blox.core.designsystem.component.widestWord
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.IrisColors
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/**
 * The choice of the game (V26), the first screen of the application: the logo, read « Choix du jeu » as its heading,
 * then [cartridges], side by side ([Cartridges]), each read by TalkBack as a button of its name, above the console
 * waiting for one, a decoration. Tapping a cartridge hands it to [onOpen], which opens its home. In Original, frozen,
 * the title is written and the cartridges bear no mascot.
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
            .padding(BloxSpaces.space4),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Title()
        Cartridges(cartridges, onOpen)
        Console()
    }
}

/**
 * The [cartridges], side by side while the name of each holds in its cartridge at the smallest size it may shrink to,
 * one above the other otherwise, as in Original in a large font. Tapping one hands it to [onOpen].
 */
@Composable
private fun Cartridges(
    cartridges: List<Cartridge>,
    onOpen: (Cartridge) -> Unit,
) {
    val gap = BloxSpaces.space4
    val measurer = rememberTextMeasurer()
    val style = cartridgeNameStyle()
    val names = cartridges.map { stringResource(it.label) }
    val density = LocalDensity.current
    // Each name at the smallest size it may shrink to, measured once for this font and density.
    val widest = remember(measurer, style, density, names) {
        val smallest = style.copy(fontSize = density.shrinkFloor(style.fontSize))
        names.map { measurer.widestWord(it, smallest) }
    }
    // The line of every name as high as the highest name at full size: the cartridges stand as high as each other.
    val nameHeight = remember(measurer, style, density, names) {
        with(density) { names.maxOf { measurer.measure(it, style).size.height }.toDp() }
    }
    BoxWithConstraints(
        modifier = Modifier.widthIn(max = CARTRIDGE_WIDTH * cartridges.size + gap * (cartridges.size - 1)),
    ) {
        val sideBySide = with(density) {
            holdSideBySide(
                width = constraints.maxWidth,
                gap = gap.roundToPx(),
                padding = CARTRIDGE_PADDING.roundToPx(),
                weights = cartridges.map { 1f },
                labels = widest,
            )
        }
        // Taps count only while this screen is resumed: once a home is on its way, a second tap does nothing.
        if (sideBySide) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                cartridges.forEach { cartridge ->
                    CartridgeCard(cartridge, nameHeight, dropUnlessResumed { onOpen(cartridge) }, Modifier.weight(1f))
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                cartridges.forEach { cartridge ->
                    CartridgeCard(
                        cartridge = cartridge,
                        nameHeight = nameHeight,
                        onClick = dropUnlessResumed { onOpen(cartridge) },
                        modifier = Modifier.widthIn(max = CARTRIDGE_WIDTH).fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** The title of the screen, its heading for TalkBack: the logo BloX in Colorful, written in Original. */
@Composable
private fun Title() {
    val title = stringResource(R.string.feature_cartridges_title)
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = TITLE_BAR_HEIGHT),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (BloxTheme.variant == BloxThemeVariant.ORIGINAL) {
            Text(
                text = title,
                color = BloxTheme.palette.ink,
                style = MaterialTheme.typography.headlineMedium.merge(BloxTheme.typography.screenTitle),
                modifier = Modifier.semantics { heading() },
            )
        } else {
            BloxLogo(
                height = LOGO_HEIGHT,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = title
                    heading()
                },
            )
        }
    }
}

/**
 * A cartridge (V26): a bubble button in the corners of a card, its grip at the top, its window in the `iris` gradient
 * where the mascot of its game stands (none in Original), its name, and its pins at the bottom. It goes down as a
 * bubble button when pressed. One button for TalkBack, read by its name, on a line [nameHeight] high.
 */
@Composable
private fun CartridgeCard(
    cartridge: Cartridge,
    nameHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shapes = BloxTheme.shapes
    BubbleButton(
        family = ButtonFamily.ICON,
        onClick = onClick,
        shape = shapes.card,
        modifier = modifier.testTag(cartridge.tag),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = CARTRIDGE_PADDING, end = CARTRIDGE_PADDING, top = BloxSpaces.space2)
                .padding(bottom = CARTRIDGE_PADDING),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(GRIP_WIDTH)
                    .height(GRIP_HEIGHT)
                    .background(palette.panelBorder, shapes.pill),
            )
            val iris = remember(palette.iris) { irisBrush(palette.iris) }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shapes.window)
                    .background(iris, shapes.window)
                    .border(BloxBorders.frame, palette.buttonOutline, shapes.window),
                contentAlignment = Alignment.Center,
            ) {
                if (BloxTheme.variant != BloxThemeVariant.ORIGINAL) {
                    Mascot(
                        character = cartridge.mascot,
                        cellSize = maxWidth * MASCOT_CELL,
                        modifier = Modifier.testTag(cartridge.mascotTag),
                    )
                }
            }
            CartridgeName(cartridge, nameHeight)
            Pins()
        }
    }
}

/**
 * The name of [cartridge], on one line, whole: in a large font, or on a narrow cartridge, it shrinks rather than being
 * cut inside a word. Its line is [height] high at least, that of the highest name at full size, so that every cartridge
 * stands as high as the others; never less than the line of its style, which it would overflow.
 */
@Composable
private fun CartridgeName(
    cartridge: Cartridge,
    height: Dp,
) {
    val style = cartridgeNameStyle()
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = height),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(cartridge.label),
            style = style,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = shrinkToFit(style.fontSize),
        )
    }
}

/** The style of the name of a cartridge: that of Jouer. */
@Composable
@ReadOnlyComposable
private fun cartridgeNameStyle(): TextStyle = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.playLabel)

/** The pins at the bottom of a cartridge, which go into the console: decorative. */
@Composable
private fun Pins() {
    Row(
        modifier = Modifier.fillMaxWidth(PINS_WIDTH).height(PIN_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(PIN_GAP),
    ) {
        repeat(PINS) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(BloxTheme.palette.panelBorder, BloxTheme.shapes.cell),
            )
        }
    }
}

/** The `iris` gradient at 135°, from its start to its end, its middle at 55 %: the window of a cartridge. */
private fun irisBrush(iris: IrisColors): Brush =
    Brush.linearGradient(0f to iris.start, IRIS_MIDDLE to iris.middle, 1f to iris.end)

private const val IRIS_MIDDLE = 0.55f

/** The widest a cartridge stands, on a tablet as on a phone, as on the mockup. */
private val CARTRIDGE_WIDTH = 220.dp

/** Between the sides of a cartridge and what it bears, its name among them. */
private val CARTRIDGE_PADDING = BloxSpaces.space3

/** The bar of the title, as high as the 56 dp bar of the mockup (V26), and the logo in it, 42 dp as there. */
private val TITLE_BAR_HEIGHT = 56.dp
private val LOGO_HEIGHT = 42.dp

/** The grip, the window and the pins of a cartridge, in fractions of its width as on the mockup (220 dp). */
private const val GRIP_WIDTH = 90f / 220f
private val GRIP_HEIGHT = 8.dp

/** A cell of the mascot, a fifth of the window: 38 dp in a window of 188 on the mockup. */
private const val MASCOT_CELL = 38f / 188f
private const val PINS = 5
private const val PINS_WIDTH = 80f / 220f
private val PIN_HEIGHT = 12.dp
private val PIN_GAP = BloxSpaces.space1

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
