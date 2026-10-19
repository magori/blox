package ch.magori.blox.feature.game.topbar

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.feature.game.controls.KeyColors
import ch.magori.blox.feature.game.controls.keyAtRestColors

/** The pause button wears the colours of a key at rest: one look for every button, framed to stand out by 3:1. */
internal fun pauseButtonColors(palette: BloxPalette): KeyColors = keyAtRestColors(palette)
