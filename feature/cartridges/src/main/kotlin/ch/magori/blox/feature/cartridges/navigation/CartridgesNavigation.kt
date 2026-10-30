package ch.magori.blox.feature.cartridges.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.CartridgesScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** Clé du choix du jeu, les cartouches (vue V36), à la racine de la pile (ADR 0022). */
@Serializable
data object CartridgesRoute : NavKey

/**
 * Déclare l'entrée du choix du jeu (ADR 0011, 0022) : chaque [Cartridge], dans l'ordre de ses entrées. [newCartridges]
 * donne celles qui portent l'autocollant « NOUVELLE », et [currentNewCartridges] celles qui le portent à l'instant,
 * valeur de la première image : l'écran qui revient le montre dès cette image, sans clignoter. `app` les tire des
 * résultats de la Graine du jour et de la date, que cette feature ne connaît pas. [onOpen] reçoit la cartouche touchée,
 * dont `app` ouvre le jeu. De même, [padlockFrontier] donne la frontière du Serrurier, la première serrure non ouverte,
 * que sa cartouche nomme (« Serrure N »), nulle tant que l'avancement n'est pas lu : `app` la tire de l'avancement des
 * serrures, que cette feature ne connaît pas. Sans ViewModel : la liste est statique, l'autocollant et la sous-ligne
 * viennent de flux. [onSettings] reçoit le toucher sur la roue dentée, dont `app` ouvre les Paramètres (ADR 0030).
 */
fun EntryProviderScope<NavKey>.cartridgesEntry(
    newCartridges: Flow<Set<Cartridge>>,
    currentNewCartridges: () -> Set<Cartridge>,
    padlockFrontier: StateFlow<Int?>,
    onOpen: (Cartridge) -> Unit,
    onSettings: () -> Unit,
) {
    entry<CartridgesRoute> {
        val new by newCartridges.collectAsStateWithLifecycle(initialValue = remember { currentNewCartridges() })
        val frontier by padlockFrontier.collectAsStateWithLifecycle()
        CartridgesScreen(
            cartridges = Cartridge.entries,
            newCartridges = new,
            padlockFrontier = frontier,
            onOpen = onOpen,
            onSettings = onSettings,
        )
    }
}
