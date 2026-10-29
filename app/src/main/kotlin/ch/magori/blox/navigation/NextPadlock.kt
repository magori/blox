package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute

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

/** Puts [by] in the place of the [open] padlock, if it is still on top of [backStack]. */
private fun replacePadlock(
    backStack: MutableList<NavKey>,
    open: LocksmithRoute,
    by: LocksmithRoute,
) {
    if (backStack.lastOrNull() != open) return
    backStack[backStack.lastIndex] = by
}
