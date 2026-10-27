package ch.magori.blox.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.navigation3.runtime.NavKey
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute

/**
 * Gives the system bars the style of the variant drawn under them ([systemBarVariant]): that of the theme drawn, or the
 * Colorful one of [darkSystem] under the Locksmith, the screen on [top] of the stack. Again whenever it changes: the
 * player picks another theme, opens the Locksmith, or the mode of the system changes. [top] is read here alone, so that
 * a change of screen recomposes these bars, not the navigation. Left to itself, `enableEdgeToEdge` follows the mode of
 * the system, and Original, light day and night, would get light icons on its light page at night.
 */
@Composable
internal fun BloxSystemBars(
    top: () -> NavKey?,
    darkSystem: Boolean,
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val darkIcons = hasDarkSystemBarIcons(systemBarVariant(top(), BloxTheme.variant, darkSystem))
    SideEffect { activity.showSystemBars(darkIcons) }
}

/**
 * The variant whose page the system bars lie on, with [top] on top of the stack: the Colorful variant of the mode of the
 * system, [darkSystem], under the Locksmith, which is drawn in the Colorful theme only; else the [chosen] variant.
 */
internal fun systemBarVariant(
    top: NavKey?,
    chosen: BloxThemeVariant,
    darkSystem: Boolean,
): BloxThemeVariant = if (top is LocksmithRoute) BloxThemeVariant.of(BloxThemeKind.COLORFUL, darkSystem) else chosen

/** Whether the icons of the system bars are dark, to be read on the page of [variant]: dark on a light page. */
internal fun hasDarkSystemBarIcons(variant: BloxThemeVariant): Boolean = !variant.isDark

/**
 * Draws the window edge to edge, the icons of its bars dark or light as [darkIcons] says, whatever the mode of the
 * system. [LIGHT_SCRIM] and [DARK_SCRIM], those of `enableEdgeToEdge` by default, serve only on API 26 to 28: from
 * API 29, `auto` leaves the navigation bar transparent and the system draws its own scrim (three buttons).
 */
internal fun ComponentActivity.showSystemBars(darkIcons: Boolean) {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { !darkIcons },
        navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { !darkIcons },
    )
}

// ARGB literals: `Color.argb` would make this file unreadable by the JVM tests, which have no Android.
private const val LIGHT_SCRIM = 0xE6FFFFFF.toInt()
private const val DARK_SCRIM = 0x801B1B1B.toInt()
