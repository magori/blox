package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.icon.BloxIcon

/**
 * Paramètres, the gear that opens the settings, through [onClick]: a [GlyphButton] of 48 dp that TalkBack reads
 * « Paramètres », once.
 */
@Composable
fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlyphButton(
        icon = BloxIcon.GEAR,
        description = stringResource(R.string.core_designsystem_settings),
        onClick = onClick,
        modifier = modifier,
    )
}
