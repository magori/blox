package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** The theme chosen by the player, saved in the preferences; what is drawn is its [BloxThemeVariant] (ADR 0020). */
enum class BloxThemeKind { ORIGINAL, COLORFUL }

private val LocalBloxThemeVariant = staticCompositionLocalOf { BloxThemeVariant.ORIGINAL }

/**
 * The theme [kind], in its variant for the mode of the system, [darkSystem]: the Colorful theme follows it, day and
 * night. Switching the mode recreates the activity, which pauses a game in play (ADR 0020).
 */
@Composable
fun BloxTheme(
    kind: BloxThemeKind = BloxThemeKind.ORIGINAL,
    darkSystem: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val variant = BloxThemeVariant.of(kind, darkSystem)
    CompositionLocalProvider(LocalBloxThemeVariant provides variant) {
        val colorScheme = remember(variant) { colorSchemeOf(variant) }
        val typography = remember(kind) { BloxTypographies.of(kind).toMaterial() }
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

object BloxTheme {
    val palette: BloxPalette
        @Composable
        @ReadOnlyComposable
        get() = paletteOf(LocalBloxThemeVariant.current)

    /** The shapes of the current theme. */
    val shapes: BloxShapes
        @Composable
        @ReadOnlyComposable
        get() = when (kind) {
            BloxThemeKind.ORIGINAL -> BloxShapeSets.Original
            BloxThemeKind.COLORFUL -> BloxShapeSets.Colorful
        }

    /** The fonts of the current theme: titles, labels and values (see [BloxTypography.valueText]). */
    val typography: BloxTypography
        @Composable
        @ReadOnlyComposable
        get() = BloxTypographies.of(kind)

    /** The current variant, for what the palette cannot say (the styles of the pieces, in feature:game). */
    val variant: BloxThemeVariant
        @Composable
        @ReadOnlyComposable
        get() = LocalBloxThemeVariant.current

    /** The current theme, as the player chose it: its shapes, fonts and icons are the same day and night. */
    val kind: BloxThemeKind
        @Composable
        @ReadOnlyComposable
        get() = LocalBloxThemeVariant.current.kind
}

/** The palette of [variant]. */
internal fun paletteOf(variant: BloxThemeVariant): BloxPalette = when (variant) {
    BloxThemeVariant.ORIGINAL -> BloxPalettes.Original
    BloxThemeVariant.COLORFUL_LIGHT -> BloxPalettes.ColorfulLight
    BloxThemeVariant.COLORFUL_DARK -> BloxPalettes.ColorfulDark
}

/**
 * The Material colour scheme of [variant]: the colours of its palette, on the light or the dark base, for what is left
 * to Material (the colours of the errors, of the ripples).
 */
internal fun colorSchemeOf(variant: BloxThemeVariant): ColorScheme {
    val palette = paletteOf(variant)
    val base = if (variant.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = palette.accent,
        onPrimary = palette.accentInk,
        background = palette.page,
        onBackground = palette.ink,
        surface = palette.panel,
        onSurface = palette.ink,
    )
}
