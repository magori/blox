package ch.magori.blox.core.designsystem.icon

import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** How the icons of a theme are drawn. */
internal enum class IconStyle {
    /** In full pixels, 8 by 8, edges sharp: the pixel font of Original. */
    PIXELS,

    /** In rounded strokes on a grid of 24. */
    STROKES,
}

/** How the icons of the theme [kind] are drawn. */
internal fun iconStyleOf(kind: BloxThemeKind): IconStyle = when (kind) {
    BloxThemeKind.ORIGINAL -> IconStyle.PIXELS
    BloxThemeKind.COLORFUL -> IconStyle.STROKES
}
