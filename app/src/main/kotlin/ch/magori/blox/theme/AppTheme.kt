package ch.magori.blox.theme

import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The theme of the whole application: the one chosen on the home screen and saved in [preferences].
 * Read from the start, in [scope], for the whole life of the process: once the settings are read, a new
 * screen (a recreated activity) knows the theme at once. [kind] is null until then, so that no default
 * theme flashes before. As a [StateFlow], it emits a theme only when it changes, not when another
 * setting does.
 */
class AppTheme(preferences: UserPreferencesRepository, scope: CoroutineScope) {
    val kind: StateFlow<BloxThemeKind?> = preferences.preferences
        .map { themeKindOf(it.theme) }
        .stateIn(scope, SharingStarted.Eagerly, initialValue = null)
}

/**
 * The theme of the design system for the theme the player chose. The only place where a saved choice
 * becomes a theme: the screens read the theme from the composition ([ch.magori.blox.core.designsystem.theme.BloxTheme]).
 */
internal fun themeKindOf(choice: ThemeChoice): BloxThemeKind = when (choice) {
    ThemeChoice.ORIGINAL -> BloxThemeKind.ORIGINAL
    ThemeChoice.COLORFUL -> BloxThemeKind.COLORFUL
}
