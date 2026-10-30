package ch.magori.blox.navigation

import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.daily.navigation.DailyRoute
import ch.magori.blox.feature.garden.navigation.GardenRoute
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import ch.magori.blox.feature.settings.navigation.SettingsRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Opens what is tapped on the choice of the game over [backStack]: a cartridge (ADR 0022), the home of the Classic, the
 * musical garden, which reads the garden kept itself (ADR 0027), or the entry of the Graine du jour (ADR 0026), at once;
 * the [frontier] of the Locksmith, the first padlock not open (ADR 0025, docs/regles-serrurier.md, Progression), once
 * read, in [scope]; or the gear, the settings with no section of a game (ADR 0030). Only from the cartridges on top:
 * once a game or the settings are on their way, a second tap stacks nothing. Each cartridge names its home here, the
 * only place that knows the features; a new cartridge does not compile until it is given one.
 */
internal class CartridgeOpener(
    private val backStack: MutableList<NavKey>,
    private val scope: CoroutineScope,
    private val frontier: suspend () -> Int,
) {
    /** The tap on the Locksmith that waits for its frontier, if any. */
    private var waiting: Job? = null

    /** Opens the home of [cartridge]. */
    fun open(cartridge: Cartridge) = fromTheCartridges {
        when (cartridge) {
            Cartridge.CLASSIC -> backStack.add(HomeRoute)
            Cartridge.LOCKSMITH -> waiting = scope.launch { backStack.add(LocksmithRoute(level = frontier())) }
            Cartridge.GARDEN -> backStack.add(GardenRoute)
            Cartridge.DAILY -> backStack.add(DailyRoute)
        }
    }

    /** Opens the settings, with no section of a game: the gear of the choice of the game (V36). */
    fun openSettings() = fromTheCartridges { backStack.add(SettingsRoute()) }

    /**
     * Runs [stack], which stacks a screen, if the cartridges are on top. Any tap first cancels a tap on the Locksmith
     * still waiting for its frontier: the player changed their mind, and the padlock must not come over what they did
     * since.
     */
    private inline fun fromTheCartridges(stack: () -> Unit) {
        waiting?.cancel()
        waiting = null
        if (backStack.lastOrNull() == CartridgesRoute) stack()
    }
}
