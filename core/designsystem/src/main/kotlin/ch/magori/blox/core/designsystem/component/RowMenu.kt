package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.icon.BloxIcon

/**
 * The menu « … » at the end of a row of a list (Mes musiques, Mes jardins): a bare disc of 48 dp read « Plus d'actions
 * pour [name] », which opens a [BloxMenu] of its [items], in their order. Touching an item closes the menu, then does
 * what it says. The focus goes back to « … » as the menu closes without a choice, or, after an item that opens a window
 * (Renommer, Supprimer), once that window has opened ([windowOpen], even a few frames later) then closed, as
 * [BloxMenu] asks.
 */
@Composable
fun RowMenu(
    name: String,
    windowOpen: Boolean,
    modifier: Modifier = Modifier,
    items: @Composable RowMenuScope.() -> Unit,
) {
    val focus = remember { FocusRequester() }
    // An item chose a window: the focus waits for it to open, which a view model may say a frame or more later, then
    // to close.
    var awaitingWindow by remember { mutableStateOf(false) }
    var windowSeen by remember { mutableStateOf(false) }
    LaunchedEffect(windowOpen, awaitingWindow) {
        when {
            !awaitingWindow -> Unit

            windowOpen -> windowSeen = true

            windowSeen -> {
                awaitingWindow = false
                windowSeen = false
                focus.requestFocus()
            }
        }
    }
    Box(modifier) {
        var expanded by remember { mutableStateOf(false) }
        val scope = remember {
            RowMenuScope { opensWindow, onClick ->
                expanded = false
                if (opensWindow) awaitingWindow = true
                onClick()
            }
        }
        GlyphButton(
            description = stringResource(R.string.core_designsystem_row_menu, name),
            onClick = { expanded = true },
            modifier = Modifier.focusRequester(focus),
            emphasis = ButtonEmphasis.BARE,
        ) { ink -> Glyphs.more(this, ink) }
        BloxMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                focus.requestFocus()
            },
        ) { scope.items() }
    }
}

/**
 * What a [RowMenu] holds: its items, each a [BloxMenuItem] that closes the menu once touched; Renommer and Supprimer,
 * the items every row offers, bear their own words and icons, and open a window.
 */
@Stable
class RowMenuScope internal constructor(private val choose: (opensWindow: Boolean, onClick: () -> Unit) -> Unit) {
    /** An item of the list, [text] after its [icon], which opens no window (Modifier, La jouer…). */
    @Composable
    fun Item(
        text: String,
        icon: BloxIcon,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        MenuItem(text, icon, onClick, danger = false, opensWindow = false, modifier = modifier)
    }

    /** « Renommer », which opens the window of the name ([RenameWindow]). */
    @Composable
    fun RenameItem(
        onRename: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        MenuItem(
            text = stringResource(R.string.core_designsystem_rename),
            icon = BloxIcon.RENAME,
            onClick = onRename,
            modifier = modifier,
            danger = false,
            opensWindow = true,
        )
    }

    /** « Supprimer », in `danger`, which opens the window that confirms it ([DeleteWindow]). */
    @Composable
    fun DeleteItem(
        onDelete: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        MenuItem(
            text = stringResource(R.string.core_designsystem_window_delete),
            icon = BloxIcon.TRASH,
            onClick = onDelete,
            modifier = modifier,
            danger = true,
            opensWindow = true,
        )
    }

    /** An item, in `danger` when it deletes ([danger]); [opensWindow] when touching it does. */
    @Composable
    private fun MenuItem(
        text: String,
        icon: BloxIcon,
        onClick: () -> Unit,
        danger: Boolean,
        opensWindow: Boolean,
        modifier: Modifier = Modifier,
    ) {
        BloxMenuItem(
            text = text,
            icon = icon,
            onClick = { choose(opensWindow, onClick) },
            modifier = modifier,
            danger = danger,
        )
    }
}
