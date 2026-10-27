package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The tag of the icon of a [BloxMenuItem], decorative, read by the tests only. */
const val MENU_ITEM_ICON_TAG = "menu_item_icon"

/** The tag of the check of a checked [BloxMenuItem], decorative, read by the tests only. */
const val MENU_ITEM_CHECK_TAG = "menu_item_check"

/** How narrow and how wide a menu may be: wide enough for « Supprimer », its lines wrapping beyond. */
private val MENU_MIN_WIDTH = 160.dp
private val MENU_MAX_WIDTH = 280.dp

/**
 * The menu « … » of what opens it (a card, the bar of the editor), shown while [expanded]: its [content], its
 * [BloxMenuItem]s, in a panel over the screen, under the end of what opens it, or above it when there is no room under
 * it. The system back, or a touch outside it, calls [onDismissRequest]; touching an item does not close it, the screen
 * does. Put it beside what opens it, in the same box.
 */
@Composable
fun BloxMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    // Room around the panel, in the window of the menu, for its shadow, which would be cut at the edges of the window.
    val shadow = BloxTheme.glosses.menu
    val room = shadow.blur + shadow.offsetY
    val roomPx = with(LocalDensity.current) { room.roundToPx() }
    val position = remember(roomPx) { MenuPositionProvider(roomPx) }
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        MenuPanel(modifier = modifier.padding(room), content = content)
    }
}

/**
 * The panel of a menu: `panel` framed by `panel-border`, `radius-md`, its shadow `menu-shadow`, as wide as its widest
 * item between [MENU_MIN_WIDTH] and [MENU_MAX_WIDTH], scrolling when it is higher than the window. The previews draw it
 * without its window.
 */
@Composable
internal fun MenuPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.md
    val shadow = BloxTheme.glosses.menu
    Column(
        modifier = modifier
            .dropShadow(
                shape,
                Shadow(radius = shadow.blur, color = shadow.color, offset = DpOffset(0.dp, shadow.offsetY)),
            )
            .widthIn(min = MENU_MIN_WIDTH, max = MENU_MAX_WIDTH)
            .width(IntrinsicSize.Max)
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .verticalScroll(rememberScrollState())
            .padding(BloxSpaces.space1),
        content = content,
    )
}

/**
 * An item of a [BloxMenu]: [icon], decorative, then [text], read alone by TalkBack, 48 dp high at least; in `danger`,
 * icon and label, when it deletes ([danger]). Pressed, it takes the fill of a pressed button, or `danger` when it
 * deletes.
 */
@Composable
fun BloxMenuItem(
    text: String,
    icon: BloxIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val look = menuItemLook(BloxTheme.palette, danger, pressed)
    MenuRow(
        look = look,
        modifier = modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        BloxIcon(icon, Modifier.testTag(MENU_ITEM_ICON_TAG), tint = look.ink)
        MenuLabel(text, look.ink)
    }
}

/**
 * An item of a [BloxMenu] that is [checked] or not (Suivre la lecture): read by TalkBack as a check box, checked or
 * not, it bears a check in place of an icon while it is checked; touching it calls [onCheckedChange] with the other.
 */
@Composable
fun BloxMenuItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val look = menuItemLook(BloxTheme.palette, danger = false, pressed = pressed)
    MenuRow(
        look = look,
        modifier = modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
    ) {
        val box = Modifier.size(IconSizes.besideLabel)
        if (checked) CheckMark(look.ink, box.testTag(MENU_ITEM_CHECK_TAG)) else Spacer(box)
        MenuLabel(text, look.ink)
    }
}

/** A row of a menu as [look] says, 48 dp high at least, as wide as the menu: its icon, then its label. */
@Composable
private fun MenuRow(
    look: MenuItemLook,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BloxSizes.minTouchTarget)
            .background(look.fill, BloxTheme.shapes.md)
            .padding(horizontal = BloxSpaces.space3),
        horizontalArrangement = Arrangement.spacedBy(IconSizes.menuGap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** The label of an item of a menu, [text] in [ink], in the style of a choice, on as many lines as it needs. */
@Composable
private fun MenuLabel(
    text: String,
    ink: Color,
) {
    Text(
        text = text,
        color = ink,
        style = MaterialTheme.typography.labelLarge.merge(BloxTheme.typography.choice),
        modifier = Modifier.padding(vertical = BloxSpaces.space3),
    )
}

/**
 * Where the window of a menu stands: its panel where [menuPosition] says, the window [room] larger on each side for the
 * shadow of the panel.
 */
private class MenuPositionProvider(private val room: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val panel = IntSize(popupContentSize.width - 2 * room, popupContentSize.height - 2 * room)
        val position = menuPosition(anchorBounds, windowSize, panel)
        return IntOffset(position.x - room, position.y - room)
    }
}
