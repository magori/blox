package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.SettingSwitchRow
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/**
 * The settings page (V21): the back arrow and the title, as on « Mes musiques »; the section « Sons et vibrations »,
 * whose switches turn the vibrations and the sound effects of the games on or off, then a note: a game reads them
 * when it is created, so a change holds from the next game on. While the saved settings are not read ([uiState] null),
 * the page and its header show, without the switches: none flashes off, and the way back is there at once.
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState?,
    onBack: () -> Unit,
    onVibrationsChanged: (Boolean) -> Unit,
    onSoundEffectsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphButton(
                description = stringResource(R.string.feature_home_settings_back),
                onClick = onBack,
                glyph = { Glyphs.back(this, it) },
            )
            SingleLineText(
                text = stringResource(R.string.feature_home_settings),
                color = palette.ink,
                style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleLarge),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.feature_home_settings_sounds),
                color = palette.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (uiState != null) {
                SettingSwitchRow(
                    icon = BloxIcon.VIBRATE,
                    label = stringResource(R.string.feature_home_vibrations),
                    subLine = stringResource(R.string.feature_home_vibrations_sub_line),
                    checked = uiState.vibrations,
                    onCheckedChange = onVibrationsChanged,
                )
                SettingSwitchRow(
                    icon = BloxIcon.NOTE,
                    label = stringResource(R.string.feature_home_sound_effects),
                    subLine = stringResource(R.string.feature_home_sound_effects_sub_line),
                    checked = uiState.soundEffects,
                    onCheckedChange = onSoundEffectsChanged,
                )
            }
            Text(
                text = stringResource(R.string.feature_home_settings_next_game),
                color = palette.label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingsScreenPreview(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        // One setting on, the other off, as by default: both states of a switch are seen.
        SettingsScreen(
            uiState = SettingsUiState(vibrations = true, soundEffects = false),
            onBack = {},
            onVibrationsChanged = {},
            onSoundEffectsChanged = {},
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun SettingsScreenOriginalPreview() {
    SettingsScreenPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun SettingsScreenColorfulPreview() {
    SettingsScreenPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
internal fun SettingsScreenLargeFontPreview() {
    SettingsScreenPreview(BloxThemeKind.ORIGINAL)
}
