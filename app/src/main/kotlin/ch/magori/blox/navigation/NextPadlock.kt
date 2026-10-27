package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import kotlin.random.Random

/**
 * The next padlock (`feature_locksmith_next`) after the [open] one: one level up, on a seed drawn from [random], it
 * takes the place of [open] on top of [backStack]. A different key, so a new view model (ADR 0011, 0025). Once
 * [open] is no longer on top (a second tap), nothing changes.
 */
internal fun nextPadlock(
    backStack: MutableList<NavKey>,
    open: LocksmithRoute,
    random: Random = Random.Default,
) {
    if (backStack.lastOrNull() != open) return
    backStack[backStack.lastIndex] = open.next(seed = random.nextLong())
}
