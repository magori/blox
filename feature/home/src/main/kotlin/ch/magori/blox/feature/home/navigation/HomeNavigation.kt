package ch.magori.blox.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.feature.home.HomeScreen
import kotlinx.serialization.Serializable

/** Clé de l'écran d'accueil (vue V1). */
@Serializable
data object HomeRoute : NavKey

/** Déclare l'entrée Accueil dans le fournisseur d'entrées de l'application (ADR 0011). */
fun EntryProviderScope<NavKey>.homeEntry(onPlay: () -> Unit) {
    entry<HomeRoute> {
        HomeScreen(onPlay = onPlay)
    }
}
