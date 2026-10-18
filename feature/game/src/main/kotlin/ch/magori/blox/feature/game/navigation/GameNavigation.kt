package ch.magori.blox.feature.game.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.game.GameScreen
import kotlinx.serialization.Serializable

/**
 * Clé de l'écran de jeu (vues V2 et V3). Elle portera le niveau de départ et la graine du
 * tirage quand le jeu existera : une partie = une clé distincte, donc un ViewModel neuf
 * (ADR 0011).
 */
@Serializable
data object GameRoute : NavKey

/** Déclare l'entrée Jeu dans le fournisseur d'entrées de l'application (ADR 0011). */
fun EntryProviderScope<NavKey>.gameEntry(onBack: () -> Unit) {
    entry<GameRoute> {
        GameScreen(onBack = onBack)
    }
}
