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

private val LocalBloxEffects = staticCompositionLocalOf { BloxEffects.Full }

/**
 * The theme [kind], in its variant for the mode of the system, [darkSystem]: the Colorful theme follows it, day and
 * night. Switching the mode recreates the activity, which pauses a game in play (ADR 0020). Its [effects], full or
 * reduced, are those every screen reads (ADR 0024).
 */
@Composable
fun BloxTheme(
    kind: BloxThemeKind = BloxThemeKind.ORIGINAL,
    darkSystem: Boolean = isSystemInDarkTheme(),
    effects: BloxEffects = BloxEffects.Full,
    content: @Composable () -> Unit,
) {
    val variant = BloxThemeVariant.of(kind, darkSystem)
    CompositionLocalProvider(LocalBloxThemeVariant provides variant, LocalBloxEffects provides effects) {
        val colorScheme = remember(variant) { colorSchemeOf(variant) }
        val typography = remember(kind) { BloxTypographies.of(kind).toMaterial() }
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/**
 * [content] in the Colorful theme, in its variant for the mode of the system, [darkSystem], whatever the theme around
 * it: the screens drawn in the Colorful theme only (ADR 0026, ADR 0027, ADR 0030). The effects around it are kept:
 * imposing a theme does not bring back the full effects of a player who reduced them.
 */
@Composable
fun ColorfulTheme(
    darkSystem: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    BloxTheme(kind = BloxThemeKind.COLORFUL, darkSystem = darkSystem, effects = BloxTheme.effects, content = content)
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
        get() = shapesOf(kind)

    /** The reliefs of the current variant: their light and shade differ day and night, and the Original is flat. */
    val glosses: BloxGlosses
        @Composable
        @ReadOnlyComposable
        get() = glossesOf(LocalBloxThemeVariant.current)

    /** The colours of the bubble buttons of the current variant. */
    val buttons: BloxButtonColors
        @Composable
        @ReadOnlyComposable
        get() = buttonColorsOf(LocalBloxThemeVariant.current)

    /** The fonts of the current theme: titles, labels and values. */
    val typography: BloxTypography
        @Composable
        @ReadOnlyComposable
        get() = BloxTypographies.of(kind)

    /** The current variant, for what the palette cannot say (the styles of the pieces, in feature:game). */
    val variant: BloxThemeVariant
        @Composable
        @ReadOnlyComposable
        get() = LocalBloxThemeVariant.current

    /**
     * The effects of the interface, full or reduced, as the player chose them or as Android imposes them: what a
     * screen reads before it moves anything (ADR 0024).
     */
    val effects: BloxEffects
        @Composable
        @ReadOnlyComposable
        get() = LocalBloxEffects.current

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

/** The shapes of the theme [kind], the same day and night. */
internal fun shapesOf(kind: BloxThemeKind): BloxShapes = when (kind) {
    BloxThemeKind.ORIGINAL -> BloxShapeSets.Original
    BloxThemeKind.COLORFUL -> BloxShapeSets.Colorful
}

/** The reliefs of [variant]. */
internal fun glossesOf(variant: BloxThemeVariant): BloxGlosses = when (variant) {
    BloxThemeVariant.ORIGINAL -> BloxGlossSets.Original
    BloxThemeVariant.COLORFUL_LIGHT -> BloxGlossSets.ColorfulLight
    BloxThemeVariant.COLORFUL_DARK -> BloxGlossSets.ColorfulDark
}

/** The colours of the bubble buttons of [variant]. */
internal fun buttonColorsOf(variant: BloxThemeVariant): BloxButtonColors = when (variant) {
    BloxThemeVariant.ORIGINAL -> BloxButtonColorSets.Original
    BloxThemeVariant.COLORFUL_LIGHT -> BloxButtonColorSets.ColorfulLight
    BloxThemeVariant.COLORFUL_DARK -> BloxButtonColorSets.ColorfulDark
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
