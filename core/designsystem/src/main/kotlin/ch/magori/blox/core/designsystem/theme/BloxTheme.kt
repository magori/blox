package ch.magori.blox.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class BloxThemeKind { ORIGINAL, COLORFUL }

private val LocalBloxPalette = staticCompositionLocalOf { BloxPalettes.Original }
private val LocalBloxThemeKind = staticCompositionLocalOf { BloxThemeKind.ORIGINAL }

@Composable
fun BloxTheme(
    kind: BloxThemeKind = BloxThemeKind.ORIGINAL,
    content: @Composable () -> Unit,
) {
    val palette = when (kind) {
        BloxThemeKind.ORIGINAL -> BloxPalettes.Original
        BloxThemeKind.COLORFUL -> BloxPalettes.Colorful
    }
    CompositionLocalProvider(LocalBloxPalette provides palette, LocalBloxThemeKind provides kind) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(kind),
            content = content,
        )
    }
}

object BloxTheme {
    val palette: BloxPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalBloxPalette.current

    /** The shapes of the current theme. */
    val shapes: BloxShapes
        @Composable
        @ReadOnlyComposable
        get() = when (LocalBloxThemeKind.current) {
            BloxThemeKind.ORIGINAL -> BloxShapeSets.Original
            BloxThemeKind.COLORFUL -> BloxShapeSets.Colorful
        }

    /** The current theme, for what the palette cannot say (the styles of the pieces, in feature:game). */
    val kind: BloxThemeKind
        @Composable
        @ReadOnlyComposable
        get() = LocalBloxThemeKind.current
}

private fun BloxPalette.toColorScheme(kind: BloxThemeKind): ColorScheme = when (kind) {
    BloxThemeKind.ORIGINAL -> lightColorScheme(
        primary = ink,
        onPrimary = accentInk,
        background = page,
        onBackground = ink,
        surface = panel,
        onSurface = ink,
    )

    BloxThemeKind.COLORFUL -> darkColorScheme(
        primary = accent,
        onPrimary = accentInk,
        background = page,
        onBackground = ink,
        surface = panel,
        onSurface = ink,
    )
}
