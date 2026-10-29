package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import ch.magori.blox.feature.locksmith.navigation.PadlocksRoute

/**
 * The next padlock (`feature_locksmith_next`) after the [open] one: one level up, it takes the place of [open] on top
 * of [backStack]. A different key, so a new view model (ADR 0011, 0025). Once [open] is no longer on top (a second
 * tap), nothing changes.
 */
internal fun nextPadlock(
    backStack: MutableList<NavKey>,
    open: LocksmithRoute,
) {
    replacePadlock(backStack, open, open.next())
}

/**
 * « Reprendre à la serrure N » (`feature_locksmith_resume`), after the replay of an old padlock: the [frontier] takes
 * the place of the [open] padlock on top of [backStack], as the next padlock does. Once [open] is no longer on top (a
 * second tap), nothing changes.
 */
internal fun resumePadlocks(
    backStack: MutableList<NavKey>,
    open: LocksmithRoute,
    frontier: LocksmithRoute,
) {
    replacePadlock(backStack, open, frontier)
}

/**
 * The disc « Toutes les serrures » of the bar of the [open] padlock: the screen of the padlocks over it, which tells
 * that padlock « en cours ». The padlock stays under it, its view model with it: back finds its pieces placed. Once
 * [open] is no longer on top (a second tap), nothing changes.
 */
internal fun openPadlocks(
    backStack: MutableList<NavKey>,
    open: LocksmithRoute,
) = backStack.whileOnTop(open) {
    add(PadlocksRoute(current = open.level))
}

/**
 * A padlock [chosen] on the screen of the padlocks, [list]: it takes the place of the padlock under it, and the list
 * goes; with no padlock under it, it takes the place of the list. When [chosen] is the padlock under the list (an equal
 * key, another instance, as the screen makes it), that key is kept, not replaced: its view model, and its pieces placed,
 * stay. Once [list] is no longer on top (a second tap), nothing changes.
 */
internal fun choosePadlock(
    backStack: MutableList<NavKey>,
    list: PadlocksRoute,
    chosen: LocksmithRoute,
) = backStack.whileOnTop(list) {
    removeAt(lastIndex)
    when (lastOrNull()) {
        chosen -> Unit
        is LocksmithRoute -> set(lastIndex, chosen)
        else -> add(chosen)
    }
}

/**
 * The progress cleared on the screen of the padlocks, [list] (P7.37): the list goes, and padlock 1 takes the place of
 * the padlock under it, over the cartridges, or the place of the list when no padlock is under it. Once [list] is no
 * longer on top (the clear told twice), nothing changes.
 */
internal fun progressCleared(
    backStack: MutableList<NavKey>,
    list: PadlocksRoute,
) = backStack.whileOnTop(list) {
    removeAt(lastIndex)
    if (lastOrNull() is LocksmithRoute) removeAt(lastIndex)
    add(LocksmithRoute(level = 1))
}

/** Puts [by] in the place of the [open] padlock, if it is still on top of [backStack]. */
private fun replacePadlock(
    backStack: MutableList<NavKey>,
    open: LocksmithRoute,
    by: LocksmithRoute,
) = backStack.whileOnTop(open) {
    set(lastIndex, by)
}

/** Makes [change] only while [key] is on top of the stack: a second tap, once the first moved it, changes nothing. */
private inline fun MutableList<NavKey>.whileOnTop(
    key: NavKey,
    change: MutableList<NavKey>.() -> Unit,
) {
    if (lastOrNull() == key) change()
}
