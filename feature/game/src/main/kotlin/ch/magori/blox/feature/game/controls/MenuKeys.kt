package ch.magori.blox.feature.game.controls

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType

/** The screens that take the place of the game: the pause, and the game over. */
internal enum class Menu { PAUSE, GAME_OVER }

/** What a key event does on a [Menu]: see [menuKeyAction]. */
internal sealed interface MenuKeyAction {
    /** The paused game resumes. */
    data object Resume : MenuKeyAction

    /** Nothing: the key is kept from the system, which would otherwise act on it. */
    data object Ignored : MenuKeyAction

    /** The focus moves from the menu itself into its buttons, as an arrow does when nothing has the focus. */
    data object EnterButtons : MenuKeyAction
}

/** The arrows of a keyboard and the cross of a pad, which move between the buttons of a menu. */
private val arrows: Set<Key> = setOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown)

/** The keys that move between the buttons of a menu and press them (A of a pad, whose fallback is the centre). */
private val menuKeys: Set<Key> = arrows + Key.ButtonA

/**
 * What [key] does on [menu], going down or coming back up ([type]) after [repeatCount] repeats, while the
 * menu itself holds the focus ([menuFocused]) or one of its buttons does.
 *
 * The menu takes the focus as it shows, so that its keys reach it: while it holds it, an arrow moves it into
 * the buttons. In pause, a pause key of the game (Start, Escape, P) resumes it, as it paused it. Once the game
 * is over, the other keys of the game do nothing, so that a player still hammering them does not leave the
 * score unseen: B would go back, Start would press a button. Null for a key left to the system: on both
 * screens the arrows, A, Tab and Enter move between the buttons and press them, and the system back does
 * what it does there (it resumes the paused game, it leaves the game over).
 */
internal fun menuKeyAction(
    menu: Menu,
    key: Key,
    type: KeyEventType,
    repeatCount: Int,
    menuFocused: Boolean,
): MenuKeyAction? {
    val firstDown = type == KeyEventType.KeyDown && repeatCount == 0
    return when {
        menuFocused && type == KeyEventType.KeyDown && key in arrows -> MenuKeyAction.EnterButtons
        menu == Menu.PAUSE && isPauseKey(key) -> if (firstDown) MenuKeyAction.Resume else MenuKeyAction.Ignored
        menu == Menu.GAME_OVER && isGameKey(key) && key !in menuKeys -> MenuKeyAction.Ignored
        else -> null
    }
}

/**
 * A key event on [menu]: what [menuKeyAction] makes of it, the resume given to [onResume], the entry into
 * the buttons to [onEnterButtons]. True when the key is kept from the system, false when it is left to it.
 */
internal fun menuKey(
    menu: Menu,
    key: Key,
    type: KeyEventType,
    repeatCount: Int,
    menuFocused: Boolean,
    onResume: () -> Unit,
    onEnterButtons: () -> Unit,
): Boolean {
    when (menuKeyAction(menu, key, type, repeatCount, menuFocused)) {
        null -> return false
        MenuKeyAction.Resume -> onResume()
        MenuKeyAction.EnterButtons -> onEnterButtons()
        MenuKeyAction.Ignored -> Unit
    }
    return true
}
