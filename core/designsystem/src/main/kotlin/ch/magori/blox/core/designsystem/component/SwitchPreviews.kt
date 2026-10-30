package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** A row of the sheet: its label, on or not, pressed or not, focused or not, and the reason it is disabled, if any. */
private data class SheetRow(
    val label: String,
    val checked: Boolean,
    val pressed: Boolean = false,
    val focused: Boolean = false,
    val disabledReason: String? = null,
)

/** The states a sheet shows: on and off, at rest and pressed, focused, then disabled on and off. */
private val SheetRows = listOf(
    SheetRow("Effets sonores", checked = true),
    SheetRow("Vibrations", checked = false),
    SheetRow("Allumé, pressé", checked = true, pressed = true),
    SheetRow("Éteint, pressé", checked = false, pressed = true),
    SheetRow("Focus", checked = false, focused = true),
    SheetRow("Effets réduits", checked = true, disabledReason = "Imposé par les réglages d’Android"),
    SheetRow("Vibration des touches", checked = false, disabledReason = "Cet appareil ne vibre pas"),
)

/**
 * The settings, each in a state of [SheetRows], on the page: in Colorful alone, as the settings page, the only one to
 * show them, is drawn whatever the theme chosen.
 */
@Composable
private fun SwitchSheet() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Column(
            modifier = Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(BloxSpaces.space4),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
        ) {
            SheetRows.forEach { row ->
                SettingSwitchRow(
                    pictogram = { tint ->
                        BloxIcon(
                            if (row.checked) BloxIcon.NOTE else BloxIcon.VIBRATE,
                            size = IconSizes.alone,
                            tint = tint,
                        )
                    },
                    label = row.label,
                    subLine = "Rotation, lignes, fin de partie",
                    checked = row.checked,
                    onCheckedChange = {},
                    depth = { if (row.pressed) 1f else 0f },
                    focused = row.focused,
                    interactionSource = null,
                    disabledReason = row.disabledReason,
                )
            }
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 720)
@Composable
internal fun SwitchesColorfulPreview() {
    SwitchSheet()
}

@Preview(widthDp = 360, heightDp = 1600, fontScale = 2f)
@Composable
internal fun SwitchesColorfulLargeFontPreview() {
    SwitchSheet()
}
