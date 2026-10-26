package ch.magori.blox.feature.cartridges.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.cartridges.Cartridge
import ch.magori.blox.feature.cartridges.CartridgesScreen
import kotlinx.serialization.Serializable

/** Clé du choix du jeu, les cartouches (vue V26), à la racine de la pile (ADR 0022). */
@Serializable
data object CartridgesRoute : NavKey

/**
 * Déclare l'entrée du choix du jeu (ADR 0011, 0022) : chaque [Cartridge], dans l'ordre de ses entrées. [onOpen] reçoit
 * la cartouche touchée, dont `app` ouvre l'accueil, que cette feature ne connaît pas. Sans ViewModel : la liste est
 * statique.
 */
fun EntryProviderScope<NavKey>.cartridgesEntry(onOpen: (Cartridge) -> Unit) {
    entry<CartridgesRoute> {
        CartridgesScreen(cartridges = Cartridge.entries, onOpen = onOpen)
    }
}
