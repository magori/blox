package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.ConfirmWindow
import ch.magori.blox.core.designsystem.component.DangerButton
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.SettingActionRow
import ch.magori.blox.core.designsystem.component.SettingSwitchRow
import ch.magori.blox.core.designsystem.component.SettingVolumeRow
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.component.WindowDialog
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/**
 * The settings page (V21, V31): the back arrow and the title, in `screen-title`, then a card per section.
 * « Affichage »: Effets réduits, imposed by Android when it removes the animations, and where the theme is chosen.
 * « Sons et vibrations »: the sound effects and their volume, greyed while they are off, the volume of the music, the
 * vibrations and the vibration of the keys, both greyed on a device without a vibrator, which they say; then a note: a
 * game reads them when it is created, so a change holds from the next game on. « Record »: Effacer, which asks first,
 * in a window whose safe answer comes first, then says « Record effacé ». While the saved settings are not read
 * ([uiState] null), the page and its header show, without the settings: none flashes off, and the way back is there at
 * once. No logic here: each gesture goes to its callback of [actions].
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState?,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(BloxSpaces.space4),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
    ) {
        Header(onBack = actions.onBack)
        if (uiState != null) {
            Display(uiState.reducedEffects, actions.onReducedEffectsChanged)
            SoundsAndVibrations(uiState, actions)
            RecordSection(cleared = uiState.recordClearing == RecordClearing.CLEARED, onClear = actions.onClearRecord)
            RecordWindow(uiState.recordClearing, actions)
        }
    }
}

/** The back arrow and the title of the page, a heading for TalkBack. */
@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
    ) {
        GlyphButton(
            description = stringResource(R.string.feature_home_settings_back),
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        SingleLineText(
            text = stringResource(R.string.feature_home_settings),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.screenTitle),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}

/**
 * « Affichage »: Effets réduits, as the player chose, but on and disabled while Android removes the animations, which
 * it says (the effects of the theme, [BloxTheme.effects]): the choice is then neither shown nor touched, and comes back
 * once Android animates again. Then the note: the theme is chosen on the home screen.
 */
@Composable
private fun Display(
    reducedEffects: Boolean,
    onReducedEffectsChanged: (Boolean) -> Unit,
) {
    val forced = BloxTheme.effects.forcedBySystem
    Section(title = stringResource(R.string.feature_home_settings_display)) {
        Card {
            SettingSwitchRow(
                icon = BloxIcon.REDUCED_EFFECTS,
                label = stringResource(R.string.feature_home_reduced_effects),
                subLine = stringResource(R.string.feature_home_reduced_effects_sub_line),
                checked = reducedEffects || forced,
                onCheckedChange = onReducedEffectsChanged,
                disabledReason = if (forced) stringResource(R.string.feature_home_reduced_effects_forced) else null,
            )
        }
        Note(stringResource(R.string.feature_home_settings_theme_note))
    }
}

/** « Sons et vibrations », in the order of the design system, then the note on the next game. */
@Composable
private fun SoundsAndVibrations(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    val noVibrator = if (uiState.canVibrate) null else stringResource(R.string.feature_home_no_vibrator)
    Section(title = stringResource(R.string.feature_home_settings_sounds)) {
        Card {
            SettingSwitchRow(
                icon = BloxIcon.NOTE,
                label = stringResource(R.string.feature_home_sound_effects),
                subLine = stringResource(R.string.feature_home_sound_effects_sub_line),
                checked = uiState.soundEffects,
                onCheckedChange = actions.onSoundEffectsChanged,
            )
            Separator()
            SettingVolumeRow(
                icon = BloxIcon.VOLUME,
                label = stringResource(R.string.feature_home_effects_volume),
                percent = uiState.effectsVolume,
                onPercentChange = actions.onEffectsVolumeChanged,
                enabled = uiState.soundEffects,
            )
            Separator()
            SettingVolumeRow(
                icon = BloxIcon.VOLUME,
                label = stringResource(R.string.feature_home_music_volume),
                percent = uiState.musicVolume,
                onPercentChange = actions.onMusicVolumeChanged,
            )
            Separator()
            SettingSwitchRow(
                icon = BloxIcon.VIBRATE,
                label = stringResource(R.string.feature_home_vibrations),
                subLine = stringResource(R.string.feature_home_vibrations_sub_line),
                checked = uiState.vibrations,
                onCheckedChange = actions.onVibrationsChanged,
                disabledReason = noVibrator,
            )
            Separator()
            SettingSwitchRow(
                icon = BloxIcon.TOUCH,
                label = stringResource(R.string.feature_home_key_vibration),
                subLine = stringResource(R.string.feature_home_key_vibration_sub_line),
                checked = uiState.keyVibration,
                onCheckedChange = actions.onKeyVibrationChanged,
                disabledReason = noVibrator,
            )
        }
        Note(stringResource(R.string.feature_home_settings_next_game))
    }
}

/**
 * « Record »: the record of the Classic mode and Effacer, read once by TalkBack as « Effacer le record du Classique »;
 * under the card, a live region, silent until the record is [cleared], then « Record effacé »: there from the start, so
 * that TalkBack announces the change of its text, which the focus coming back from the window does not cover.
 */
@Composable
private fun RecordSection(
    cleared: Boolean,
    onClear: () -> Unit,
) {
    Section(title = stringResource(R.string.feature_home_settings_record)) {
        Card {
            SettingActionRow(icon = BloxIcon.TROPHY, label = stringResource(R.string.feature_home_classic_record)) {
                val name = stringResource(R.string.feature_home_clear_record_description)
                DangerButton(
                    text = stringResource(R.string.feature_home_clear_record),
                    icon = BloxIcon.TRASH,
                    onClick = onClear,
                    modifier = Modifier.semantics { contentDescription = name },
                )
            }
        }
        Box(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            if (cleared) Note(stringResource(R.string.feature_home_record_cleared))
        }
    }
}

/**
 * The window that asks whether to clear the record: while it is asked, after it failed, and while the clear is written,
 * its answers then disabled and nothing closing it (neither Back nor a touch outside); the safe answer first.
 */
@Composable
private fun RecordWindow(
    clearing: RecordClearing,
    actions: SettingsActions,
) {
    if (clearing != RecordClearing.ASKED && clearing != RecordClearing.FAILED && clearing != RecordClearing.CLEARING) {
        return
    }
    val waiting = clearing == RecordClearing.CLEARING
    WindowDialog(onDismissRequest = if (waiting) ({}) else actions.onKeepRecord) {
        ConfirmWindow(
            title = stringResource(R.string.feature_home_clear_record_title),
            dismiss = stringResource(R.string.feature_home_keep_record),
            confirm = stringResource(R.string.feature_home_clear_record_confirm),
            onConfirm = actions.onClearRecordConfirmed,
            onDismiss = actions.onKeepRecord,
            danger = true,
            failed = clearing == RecordClearing.FAILED,
            waiting = waiting,
        )
    }
}

/** A section of the page: its [title], a heading for TalkBack, then its [content]. */
@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
        Text(
            text = title,
            color = BloxTheme.palette.label,
            style = MaterialTheme.typography.labelLarge.merge(BloxTheme.typography.section),
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** A card of settings: `panel` in `radius-window`, framed by `panel-border`, its rows [BloxSpaces.space1] within. */
@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.window
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(BloxSpaces.space1),
        content = content,
    )
}

/** The line between two rows of a card: 1 dp of `panel-border`, decorative. */
@Composable
private fun Separator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = BloxSpaces.space2)
            .height(1.dp)
            .background(BloxTheme.palette.panelBorder),
    )
}

/** A note under a card, in `small` and `label`. */
@Composable
private fun Note(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = BloxTheme.palette.label,
        style = MaterialTheme.typography.bodyMedium.merge(BloxTheme.typography.small),
        modifier = modifier,
    )
}

/**
 * The page in the theme [kind], as by default but for the music, lower, and the vibrations, on: each state seen; in
 * [effects], full unless Android imposes the reduced ones.
 */
@Composable
private fun SettingsScreenPreview(
    kind: BloxThemeKind,
    uiState: SettingsUiState = PreviewState,
    effects: BloxEffects = BloxEffects.Full,
) {
    BloxTheme(kind = kind, effects = effects) {
        SettingsScreen(uiState = uiState, actions = SettingsActions.None)
    }
}

private val PreviewState = SettingsUiState(
    reducedEffects = false,
    vibrations = true,
    soundEffects = true,
    keyVibration = false,
    effectsVolume = 80,
    musicVolume = 60,
    canVibrate = true,
)

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 1200)
@Composable
internal fun SettingsScreenOriginalPreview() {
    SettingsScreenPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 1200)
@Composable
internal fun SettingsScreenColorfulPreview() {
    SettingsScreenPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 2400, fontScale = 2f)
@Composable
internal fun SettingsScreenLargeFontPreview() {
    SettingsScreenPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 2400, fontScale = 2f)
@Composable
internal fun SettingsScreenColorfulLargeFontPreview() {
    SettingsScreenPreview(BloxThemeKind.COLORFUL)
}

/**
 * Android removing the animations, without a vibrator, the sound effects off, the record just cleared: every greyed
 * setting, Effets réduits imposed, and « Record effacé ».
 */
@Preview(widthDp = 360, heightDp = 1200)
@Composable
internal fun SettingsScreenGreyedPreview() {
    SettingsScreenGreyed(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 1200)
@Composable
internal fun SettingsScreenOriginalGreyedPreview() {
    SettingsScreenGreyed(BloxThemeKind.ORIGINAL)
}

@Composable
private fun SettingsScreenGreyed(kind: BloxThemeKind) {
    SettingsScreenPreview(
        kind,
        PreviewState.copy(soundEffects = false, canVibrate = false, recordClearing = RecordClearing.CLEARED),
        effects = BloxEffects(reduced = true, forcedBySystem = true),
    )
}
