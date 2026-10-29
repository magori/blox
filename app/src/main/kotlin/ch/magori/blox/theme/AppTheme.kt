package ch.magori.blox.theme

import ch.magori.blox.WarmValue
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * How the whole application looks, as saved in the settings: the theme [kind] chosen on the home screen, and whether
 * the player asked for [reducedEffects] (Paramètres, Affichage), which Android may still impose
 * ([ch.magori.blox.core.designsystem.theme.BloxEffects.resolve]).
 */
data class AppAppearance(val kind: BloxThemeKind, val reducedEffects: Boolean)

/**
 * The appearance of the whole application, saved in [preferences]. Read from the start, in [scope], for the whole
 * life of the process: once the settings are read, a new screen (a recreated activity) knows it at once. [appearance]
 * is null until then, so that neither a default theme nor the full effects show before; the theme and the choice of
 * effects come together, from the same reading. As a [StateFlow], it emits only when one of them changes, not when
 * another setting does.
 */
class AppTheme(preferences: UserPreferencesRepository, scope: CoroutineScope) {
    val appearance: StateFlow<AppAppearance?> = WarmValue(
        preferences.preferences.map { AppAppearance(themeKindOf(it.theme), it.reducedEffects) },
        scope,
    ).current
}

/**
 * The theme of the design system for the theme the player chose. The only place where a saved choice
 * becomes a theme: the screens read the theme from the composition ([ch.magori.blox.core.designsystem.theme.BloxTheme]).
 */
internal fun themeKindOf(choice: ThemeChoice): BloxThemeKind = when (choice) {
    ThemeChoice.ORIGINAL -> BloxThemeKind.ORIGINAL
    ThemeChoice.COLORFUL -> BloxThemeKind.COLORFUL
}
