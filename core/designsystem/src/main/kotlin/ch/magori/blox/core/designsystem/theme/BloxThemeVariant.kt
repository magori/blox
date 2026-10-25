package ch.magori.blox.core.designsystem.theme

/**
 * What is drawn (ADR 0020): the theme chosen by the player, [kind], resolved with the dark mode of the system. The
 * Colorful theme follows it, day and night; the Original theme has a single variant, light.
 */
enum class BloxThemeVariant(val kind: BloxThemeKind, val isDark: Boolean) {
    ORIGINAL(BloxThemeKind.ORIGINAL, isDark = false),
    COLORFUL_LIGHT(BloxThemeKind.COLORFUL, isDark = false),
    COLORFUL_DARK(BloxThemeKind.COLORFUL, isDark = true),
    ;

    companion object {
        /** The variant of [kind] when the system is in dark mode, or not, as [darkSystem] says. */
        fun of(
            kind: BloxThemeKind,
            darkSystem: Boolean,
        ): BloxThemeVariant = when (kind) {
            BloxThemeKind.ORIGINAL -> ORIGINAL
            BloxThemeKind.COLORFUL -> if (darkSystem) COLORFUL_DARK else COLORFUL_LIGHT
        }
    }
}
