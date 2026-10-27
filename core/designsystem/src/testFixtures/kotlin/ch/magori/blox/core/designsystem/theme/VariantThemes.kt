package ch.magori.blox.core.designsystem.theme

/**
 * The palette of [variant], as `BloxTheme` gives it, for the tests of the modules that draw in a variant (core:board,
 * the features): one mapping, the theme's own.
 */
fun variantPalette(variant: BloxThemeVariant): BloxPalette = paletteOf(variant)

/** The reliefs of [variant], as `BloxTheme` gives them, for the tests of the modules that draw in a variant. */
fun variantGlosses(variant: BloxThemeVariant): BloxGlosses = glossesOf(variant)
