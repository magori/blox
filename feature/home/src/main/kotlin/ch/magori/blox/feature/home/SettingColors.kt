package ch.magori.blox.feature.home

import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.theme.BloxPalette

/** The colours of a setting of the settings page (V21): its [row], drawn as an unselected choice, and its [switch]. */
@Immutable
internal data class SettingColors(val row: ChoiceColors, val switch: SwitchColors)

/**
 * The colours of a setting in [palette]. The row is an unselected choice ([choiceColors]): `panel` framed in
 * `panelBorder`, its label in `ink`. Its switch, off, is the row itself, outlined in the colour of the label, its thumb
 * too; on, it is filled in `accent`, as a selected choice, its thumb in `accentInk`. Colour is not the only cue (WCAG
 * 2.1, 1.4.1): the thumb stands on the left when off, on the right when on. A setting is never disabled: its disabled
 * colours are its enabled ones.
 */
internal fun settingColors(palette: BloxPalette): SettingColors {
    val row = choiceColors(palette, selected = false)
    val on = palette.accent
    val onThumb = palette.accentInk
    val off = row.background
    val offLine = row.ink
    return SettingColors(
        row = row,
        switch = SwitchColors(
            checkedThumbColor = onThumb,
            checkedTrackColor = on,
            checkedBorderColor = on,
            checkedIconColor = on,
            uncheckedThumbColor = offLine,
            uncheckedTrackColor = off,
            uncheckedBorderColor = offLine,
            uncheckedIconColor = off,
            disabledCheckedThumbColor = onThumb,
            disabledCheckedTrackColor = on,
            disabledCheckedBorderColor = on,
            disabledCheckedIconColor = on,
            disabledUncheckedThumbColor = offLine,
            disabledUncheckedTrackColor = off,
            disabledUncheckedBorderColor = offLine,
            disabledUncheckedIconColor = off,
        ),
    )
}
