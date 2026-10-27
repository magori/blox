package ch.magori.blox.feature.cartridges.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.CartridgesScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Clé du choix du jeu, les cartouches (vue V36), à la racine de la pile (ADR 0022). */
@Serializable
data object CartridgesRoute : NavKey

/**
 * Déclare l'entrée du choix du jeu (ADR 0011, 0022) : chaque [Cartridge], dans l'ordre de ses entrées. [newCartridges]
 * donne celles qui portent l'autocollant « NOUVELLE », et [currentNewCartridges] celles qui le portent à l'instant,
 * valeur de la première image : l'écran qui revient le montre dès cette image, sans clignoter. `app` les tire des
 * résultats de la Graine du jour et de la date, que cette feature ne connaît pas. [onOpen] reçoit la cartouche touchée,
 * dont `app` ouvre le jeu. Sans ViewModel : la liste est statique, et l'autocollant vient d'un flux.
 */
fun EntryProviderScope<NavKey>.cartridgesEntry(
    newCartridges: Flow<Set<Cartridge>>,
    currentNewCartridges: () -> Set<Cartridge>,
    onOpen: (Cartridge) -> Unit,
) {
    entry<CartridgesRoute> {
        val new by newCartridges.collectAsStateWithLifecycle(initialValue = remember { currentNewCartridges() })
        CartridgesScreen(cartridges = Cartridge.entries, newCartridges = new, onOpen = onOpen)
    }
}
