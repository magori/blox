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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
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
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.core.designsystem.theme.IrisColors
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/**
 * The choice of the game (V36), the first screen of the application: the logo, then « Choix du jeu », its heading, then
 * the [cartridges] on two columns, the last one centred below when they are odd in number ([Cartridges]), each read by
 * TalkBack as a button of its name; those of [newCartridges] bear the sticker NOUVELLE, read after their name. Tapping a
 * cartridge hands it to [onOpen], which opens its game. Drawn in the Colorful theme whatever the theme chosen, as the
 * Serrurier and the Graine du jour: the Original theme is frozen (ADR 0026).
 */
@Composable
internal fun CartridgesScreen(
    cartridges: List<Cartridge>,
    newCartridges: Set<Cartridge>,
    onOpen: (Cartridge) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColorfulTheme { CartridgesPage(cartridges, newCartridges, onOpen, modifier) }
}

/** The page of the choice of the game, in the theme around it. */
@Composable
private fun CartridgesPage(
    cartridges: List<Cartridge>,
    newCartridges: Set<Cartridge>,
    onOpen: (Cartridge) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BloxSpaces.space4)
            .padding(top = BloxSpaces.space2, bottom = BloxSpaces.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Title()
        Cartridges(cartridges, newCartridges, onOpen)
        Text(
            text = stringResource(R.string.feature_cartridges_hint),
            color = BloxTheme.palette.label,
            style = MaterialTheme.typography.bodyMedium.merge(BloxTheme.typography.small),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = BloxSpaces.space4),
        )
    }
}

/**
 * The title of the screen: the logo BloX in its bar, a decoration; then « Choix du jeu », centred, its heading for
 * TalkBack.
 */
@Composable
private fun Title() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = TITLE_BAR_HEIGHT),
            contentAlignment = Alignment.CenterStart,
        ) {
            BloxLogo(height = LOGO_HEIGHT, modifier = Modifier.clearAndSetSemantics {})
        }
        Text(
            text = stringResource(R.string.feature_cartridges_title),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.headlineSmall.merge(BloxTheme.typography.screenTitle),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = BloxSpaces.space2, bottom = BloxSpaces.space3)
                .semantics { heading() },
        )
    }
}

/**
 * The [cartridges], two by row, as wide as each other, the last one alone centred below them when they are odd in
 * number: 3 then 5 cartridges take the same grid. One column, while the name of each does not hold in a column at the
 * smallest size it may shrink to, as in a large font on a narrow window. Those of [newCartridges] bear the sticker.
 * Tapping one hands it to [onOpen].
 */
@Composable
private fun Cartridges(
    cartridges: List<Cartridge>,
    newCartridges: Set<Cartridge>,
    onOpen: (Cartridge) -> Unit,
) {
    val gap = BloxSpaces.space4
    val measurer = rememberTextMeasurer()
    val style = cartridgeNameStyle()
    val names = cartridges.map { stringResource(it.label) }
    val density = LocalDensity.current
    // The widest word of any name at the smallest size it may shrink to, measured once for this font and density: a
    // name may go on two lines, never cut inside a word.
    val widest = remember(measurer, style, density, names) {
        val smallest = style.copy(fontSize = density.shrinkFloor(style.fontSize))
        names.maxOf { measurer.widestWord(it, smallest) }
    }
    // The name of every cartridge on a place two lines high at full size: the cartridges stand as high as each other.
    val nameHeight = remember(measurer, style, density) {
        with(density) { measurer.measure(TWO_LINES, style).size.height.toDp() }
    }
    BoxWithConstraints(modifier = Modifier.widthIn(max = CARTRIDGE_WIDTH * COLUMNS + gap)) {
        val twoColumns = with(density) {
            holdSideBySide(
                width = constraints.maxWidth,
                gap = gap.roundToPx(),
                padding = CARTRIDGE_PADDING.roundToPx(),
                weights = List(COLUMNS) { 1f },
                labels = List(COLUMNS) { widest },
            )
        }
        val columns = if (twoColumns) COLUMNS else 1
        val width = if (twoColumns) (maxWidth - gap) / COLUMNS else min(maxWidth, CARTRIDGE_WIDTH)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ROW_GAP),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            cartridges.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { cartridge ->
                        // Taps count only while this screen is resumed: once a game is on its way, a second tap does
                        // nothing.
                        CartridgeCard(
                            cartridge = cartridge,
                            new = cartridge in newCartridges,
                            nameHeight = nameHeight,
                            onClick = dropUnlessResumed { onOpen(cartridge) },
                            modifier = Modifier.width(width),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A cartridge (V36): a bubble button in the corners of a window, its grip at the top, its window in the `iris` gradient
 * where the mascot of its game stands, its name, and its pins at the bottom. It goes down as a
 * bubble button when pressed. One button for TalkBack, read by its name, then « Nouvelle » when it is [new]: it then
 * bears the sticker NOUVELLE over its top right corner.
 */
@Composable
private fun CartridgeCard(
    cartridge: Cartridge,
    new: Boolean,
    nameHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shapes = BloxTheme.shapes
    val newLabel = stringResource(R.string.feature_cartridges_new)
    Box(modifier = modifier) {
        BubbleButton(
            family = ButtonFamily.ICON,
            onClick = onClick,
            shape = shapes.window,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(cartridge.tag)
                .semantics { if (new) stateDescription = newLabel },
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = CARTRIDGE_PADDING, vertical = CARTRIDGE_EDGE),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(GRIP_WIDTH)
                        .height(GRIP_HEIGHT)
                        .background(palette.panelBorder, shapes.pill),
                )
                CartridgeWindow(cartridge, Modifier.padding(top = CARTRIDGE_EDGE, bottom = BloxSpaces.space2))
                CartridgeName(cartridge, nameHeight)
                Pins(Modifier.padding(top = BloxSpaces.space2))
            }
        }
        if (new) {
            NewSticker(newLabel, Modifier.align(Alignment.TopEnd).offset(x = STICKER_OUT_X, y = -STICKER_OUT_Y))
        }
    }
}

/** The window of a cartridge, in the `iris` gradient, where the mascot of its game stands. */
@Composable
private fun CartridgeWindow(
    cartridge: Cartridge,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.md
    val iris = remember(palette.iris) { irisBrush(palette.iris) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(WINDOW_RATIO)
            .clip(shape)
            .background(iris, shape)
            .border(BloxBorders.frame, palette.buttonOutline, shape)
            .padding(top = WINDOW_TOP),
        contentAlignment = Alignment.Center,
    ) {
        Mascot(
            character = cartridge.mascot,
            cellSize = maxWidth * MASCOT_CELL,
            modifier = Modifier.testTag(cartridge.mascotTag),
        )
    }
}

/**
 * The sticker NOUVELLE: [label] in capitals, `accent-ink` on `accent`, in the font of the stickers, in a pill framed in
 * the colour of the page that leans. A decoration: TalkBack reads its word as the state of the
 * cartridge. Shrinks rather than grow past the cartridge in a large font.
 */
@Composable
private fun NewSticker(
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.pill
    val style = BloxTheme.typography.pointsPop.copy(fontSize = STICKER_SIZE, letterSpacing = STICKER_SPACING)
    Text(
        text = label.uppercase(),
        color = palette.accentInk,
        style = style,
        maxLines = 1,
        softWrap = false,
        autoSize = shrinkToFit(style.fontSize),
        modifier = modifier
            .semantics { hideFromAccessibility() }
            .widthIn(max = STICKER_MAX_WIDTH)
            .graphicsLayer { rotationZ = STICKER_TILT }
            .background(palette.accent, shape)
            .border(BloxBorders.frame, palette.page, shape)
            .padding(horizontal = STICKER_PADDING_X, vertical = STICKER_PADDING_Y),
    )
}

/**
 * The name of [cartridge], centred, on as few lines as it can, two at most, never cut inside a word: in a large font,
 * or on a narrow cartridge, it shrinks rather than being cut. Its place is [height] high at least, two lines at full
 * size, so that every cartridge stands as high as the others.
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
            maxLines = 2,
            autoSize = shrinkToFewestLines(style.fontSize),
        )
    }
}

/** The style of the name of a cartridge: that of Jouer, at 15 sp. */
@Composable
@ReadOnlyComposable
private fun cartridgeNameStyle(): TextStyle = MaterialTheme.typography.titleMedium
    .merge(BloxTheme.typography.playLabel)
    .copy(fontSize = NAME_SIZE, lineHeight = NAME_LINE_HEIGHT)

/** The pins at the bottom of a cartridge, which go into the console: decorative. */
@Composable
private fun Pins(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(PINS_WIDTH).height(PIN_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(PIN_GAP),
    ) {
        repeat(PINS) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(BloxTheme.palette.panelBorder, BloxTheme.shapes.pill),
            )
        }
    }
}

/** The `iris` gradient at 135°, from its start to its end, its middle at 55 %: the window of a cartridge. */
private fun irisBrush(iris: IrisColors): Brush =
    Brush.linearGradient(0f to iris.start, IRIS_MIDDLE to iris.middle, 1f to iris.end)

private const val IRIS_MIDDLE = 0.55f

/** Two lines, to measure the place of a name. */
private const val TWO_LINES = "A\nA"

/** The cartridges go two by row. */
private const val COLUMNS = 2

/** The widest a cartridge stands, on a tablet as on a phone, as on the mockup (V36). */
private val CARTRIDGE_WIDTH = 171.dp

/** Between two rows of cartridges, as on the mockup. */
private val ROW_GAP = 14.dp

/** Between the sides of a cartridge and its window, its name among them. */
private val CARTRIDGE_PADDING = BloxSpaces.space3

/** Above the grip and below the pins, and between the grip and the window. */
private val CARTRIDGE_EDGE = 9.dp

/** The bar of the title, as high as the 56 dp bar of the mockup, and the logo in it, 42 dp as there. */
private val TITLE_BAR_HEIGHT = 56.dp
private val LOGO_HEIGHT = 42.dp

/** The grip, the window and the pins of a cartridge, in fractions of its inside as on the mockup (147 dp). */
private const val GRIP_WIDTH = 61f / 147f
private val GRIP_HEIGHT = 8.dp
private const val WINDOW_RATIO = 147f / 104f

/** Above the mascot in its window, the room of its accessory. */
private val WINDOW_TOP = 10.dp

/** A cell of the mascot: 20 dp in a window of 147 on the mockup. */
private const val MASCOT_CELL = 20f / 147f
private const val PINS = 5
private const val PINS_WIDTH = 71f / 147f
private val PIN_HEIGHT = 7.dp
private val PIN_GAP = 5.dp

private val NAME_SIZE = 15.sp
private val NAME_LINE_HEIGHT = 1.15.em

/** The sticker NOUVELLE, as on the mockup: 10 dp out on the right, 12 dp above, turned by -6°. */
private val STICKER_OUT_X = 10.dp
private val STICKER_OUT_Y = 12.dp
private const val STICKER_TILT = -6f
private val STICKER_SIZE = 11.sp
private val STICKER_SPACING = 0.04.em
private val STICKER_PADDING_X = 10.dp
private val STICKER_PADDING_Y = 5.dp
private val STICKER_MAX_WIDTH = 140.dp

/**
 * The choice of the game under the Original theme of the application, as at the first start: it draws itself in the
 * Colorful theme. The Graine du jour new.
 */
@Composable
private fun CartridgesPreview() {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        CartridgesScreen(cartridges = Cartridge.entries, newCartridges = setOf(Cartridge.DAILY), onOpen = {})
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun CartridgesDayPreview() {
    CartridgesPreview()
}

@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
internal fun CartridgesNightPreview() {
    CartridgesPreview()
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun CartridgesLargeFontPreview() {
    CartridgesPreview()
}
