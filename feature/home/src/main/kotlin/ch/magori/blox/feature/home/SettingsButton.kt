package ch.magori.blox.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.icon.BloxIcon

/**
 * The gear button at the top right of the home screen (V1), 48 dp, which opens the settings page (V21); TalkBack reads
 * « Paramètres », once: the button is described, not its icon.
 */
@Composable
internal fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlyphButton(
        icon = BloxIcon.GEAR,
        description = stringResource(R.string.feature_home_settings),
        onClick = onClick,
        modifier = modifier,
    )
}
