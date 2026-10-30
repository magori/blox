package ch.magori.blox.feature.settings

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
import androidx.compose.ui.platform.testTag
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
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.feature.settings.navigation.SettingsSection

/**
 * The settings page (V21, V31): the back arrow and the title, in `screen-title`, then the settings of the game of
 * [section], if any, under « Réglages du Classique » (ADR 0030), then those of all games, under « Tous les jeux », a
 * card per section.
 * « Réglages du Classique »: « Record »: Effacer, which asks first, in a window whose safe answer comes first, then says
 * « Record effacé »; then a note: the theme of the Classic is chosen on its home.
 * « Affichage »: Effets réduits, imposed by Android when it removes the animations, and Gaucher, whose sub-line names
 * the games that have keys.
 * « Sons et vibrations »: the sound effects and their volume, greyed while they are off, the volume of the music, the
 * vibrations and the vibration of the keys, both greyed on a device without a vibrator, which they say, else naming
 * the games that have keys; then a note: the volumes hold at once, the rest from the next game opened, which reads
 * them when it is created. While the saved settings are not read ([uiState] null), the page and its header show,
 * without the settings: none flashes off, and the way back is there at once. No logic here: each gesture goes to its
 * callback of [actions]. Drawn in the Colorful theme whatever the theme chosen for the Classic, with the effects of the
 * player, as the choice of the game: the Original theme is frozen (ADR 0030).
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState?,
    section: SettingsSection?,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    ColorfulTheme { SettingsPage(uiState, section, actions, modifier) }
}

/** The settings page, in the theme around it. */
@Composable
private fun SettingsPage(
    uiState: SettingsUiState?,
    section: SettingsSection?,
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
            if (section == SettingsSection.CLASSIC) {
                PartTitle(stringResource(R.string.feature_settings_classic))
                ClassicSection(uiState.recordClearing == RecordClearing.CLEARED, actions)
                RecordWindow(uiState.recordClearing, actions)
            }
            PartTitle(stringResource(R.string.feature_settings_all_games))
            Display(uiState, actions)
            SoundsAndVibrations(uiState, actions)
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
            description = stringResource(R.string.feature_settings_back),
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        SingleLineText(
            text = stringResource(R.string.feature_settings_title),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.screenTitle),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}

/**
 * « Affichage »: Effets réduits, as the player chose, but on and disabled while Android removes the animations, which
 * it says (the effects of the theme, [BloxTheme.effects]): the choice is then neither shown nor touched, and comes back
 * once Android animates again. Gaucher, whose sub-line names the games that have keys and says where their keys stand in
 * the next game, beside its sketch of them, which turns over with it.
 */
@Composable
private fun Display(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    val forced = BloxTheme.effects.forcedBySystem
    Section(title = stringResource(R.string.feature_settings_display)) {
        Card(DISPLAY_CARD_TAG) {
            SettingSwitchRow(
                icon = BloxIcon.REDUCED_EFFECTS,
                label = stringResource(R.string.feature_settings_reduced_effects),
                subLine = stringResource(R.string.feature_settings_reduced_effects_sub_line),
                checked = uiState.reducedEffects || forced,
                onCheckedChange = actions.onReducedEffectsChanged,
                disabledReason = if (forced) stringResource(R.string.feature_settings_reduced_effects_forced) else null,
            )
            Separator()
            val leftHanded = uiState.leftHanded
            SettingSwitchRow(
                pictogram = { tint -> KeysSketch(leftHanded, tint) },
                label = stringResource(R.string.feature_settings_left_handed),
                subLine = stringResource(
                    if (leftHanded) {
                        R.string.feature_settings_left_handed_on
                    } else {
                        R.string.feature_settings_left_handed_off
                    },
                    stringResource(R.string.feature_settings_games_with_keys),
                ),
                checked = leftHanded,
                onCheckedChange = actions.onLeftHandedChanged,
            )
        }
    }
}

/** « Sons et vibrations », in the order of the design system, then the note on when they hold. */
@Composable
private fun SoundsAndVibrations(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    val noVibrator = if (uiState.canVibrate) null else stringResource(R.string.feature_settings_no_vibrator)
    val gamesWithKeys = stringResource(R.string.feature_settings_games_with_keys)
    Section(title = stringResource(R.string.feature_settings_sounds)) {
        Card(SOUNDS_CARD_TAG) {
            SettingSwitchRow(
                icon = BloxIcon.NOTE,
                label = stringResource(R.string.feature_settings_sound_effects),
                subLine = stringResource(R.string.feature_settings_sound_effects_sub_line),
                checked = uiState.soundEffects,
                onCheckedChange = actions.onSoundEffectsChanged,
            )
            Separator()
            SettingVolumeRow(
                icon = BloxIcon.VOLUME,
                label = stringResource(R.string.feature_settings_effects_volume),
                percent = uiState.effectsVolume,
                onPercentChange = actions.onEffectsVolumeChanged,
                enabled = uiState.soundEffects,
            )
            Separator()
            SettingVolumeRow(
                icon = BloxIcon.VOLUME,
                label = stringResource(R.string.feature_settings_music_volume),
                percent = uiState.musicVolume,
                onPercentChange = actions.onMusicVolumeChanged,
            )
            Separator()
            SettingSwitchRow(
                icon = BloxIcon.VIBRATE,
                label = stringResource(R.string.feature_settings_vibrations),
                subLine = stringResource(R.string.feature_settings_vibrations_sub_line, gamesWithKeys),
                checked = uiState.vibrations,
                onCheckedChange = actions.onVibrationsChanged,
                disabledReason = noVibrator,
            )
            Separator()
            SettingSwitchRow(
                icon = BloxIcon.TOUCH,
                label = stringResource(R.string.feature_settings_key_vibration),
                subLine = stringResource(R.string.feature_settings_key_vibration_sub_line, gamesWithKeys),
                checked = uiState.keyVibration,
                onCheckedChange = actions.onKeyVibrationChanged,
                disabledReason = noVibrator,
            )
        }
        Note(stringResource(R.string.feature_settings_taking_effect))
    }
}

/**
 * The section of the Classic: « Record », then the note that says where its theme is chosen, last of the section,
 * [BloxSpaces.space2] under the record, as a note under its card.
 */
@Composable
private fun ClassicSection(
    cleared: Boolean,
    actions: SettingsActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
        RecordSection(cleared = cleared, onClear = actions.onClearRecord)
        Note(stringResource(R.string.feature_settings_classic_theme_note))
    }
}

/**
 * « Record »: the record of the Classic mode and Effacer, read once by TalkBack as « Effacer le record du Classique »;
 * right under the card, a live region, silent and empty until the record is [cleared], then « Record effacé »,
 * [BloxSpaces.space2] under the card: there from the start, so that TalkBack announces the change of its text, which
 * the focus coming back from the window does not cover. Empty, it adds no space under the card.
 */
@Composable
private fun RecordSection(
    cleared: Boolean,
    onClear: () -> Unit,
) {
    Section(title = stringResource(R.string.feature_settings_record)) {
        Column {
            Card(RECORD_CARD_TAG) {
                SettingActionRow(
                    icon = BloxIcon.TROPHY,
                    label = stringResource(R.string.feature_settings_classic_record),
                ) {
                    val name = stringResource(R.string.feature_settings_clear_record_description)
                    DangerButton(
                        text = stringResource(R.string.feature_settings_clear_record),
                        icon = BloxIcon.TRASH,
                        onClick = onClear,
                        modifier = Modifier.semantics { contentDescription = name },
                    )
                }
            }
            Box(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                if (cleared) {
                    Note(
                        stringResource(R.string.feature_settings_record_cleared),
                        Modifier.padding(top = BloxSpaces.space2),
                    )
                }
            }
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
            title = stringResource(R.string.feature_settings_clear_record_title),
            dismiss = stringResource(R.string.feature_settings_keep_record),
            confirm = stringResource(R.string.feature_settings_clear_record_confirm),
            onConfirm = actions.onClearRecordConfirmed,
            onDismiss = actions.onKeepRecord,
            danger = true,
            failed = clearing == RecordClearing.FAILED,
            waiting = waiting,
        )
    }
}

/**
 * The title of a part of the page, the settings of a game or those of all games, above its sections: a heading for
 * TalkBack, in `ink`, larger than the title of a section.
 */
@Composable
private fun PartTitle(text: String) {
    Text(
        text = text,
        color = BloxTheme.palette.ink,
        style = MaterialTheme.typography.titleMedium.merge(BloxTheme.typography.title),
        modifier = Modifier.semantics { heading() },
    )
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

/**
 * A card of settings: `panel` in `radius-window`, framed by `panel-border`, its rows [BloxSpaces.space1] within; found by
 * the tests by its [tag].
 */
@Composable
private fun Card(
    tag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.window
    Column(
        modifier = Modifier
            .testTag(tag)
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
 * The page, as by default but for the music, lower, and the vibrations, on: each state seen; in [effects], full unless
 * Android imposes the reduced ones. The page draws itself in the Colorful theme, whatever the theme around it.
 */
@Composable
private fun SettingsScreenPreview(
    uiState: SettingsUiState = PreviewState,
    effects: BloxEffects = BloxEffects.Full,
    section: SettingsSection? = SettingsSection.CLASSIC,
) {
    BloxTheme(effects = effects) {
        SettingsScreen(uiState = uiState, section = section, actions = SettingsActions.None)
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
internal fun SettingsScreenColorfulPreview() {
    SettingsScreenPreview()
}

/** Opened without the section of a game (the choice of the game, ADR 0030): Tous les jeux alone, no record. */
@Preview(widthDp = 360, heightDp = 1200)
@Composable
internal fun SettingsScreenAllGamesPreview() {
    SettingsScreenPreview(section = null)
}

/** For a left handed player: Gaucher on, its sketch turned over. */
@Preview(widthDp = 360, heightDp = 1200)
@Composable
internal fun SettingsScreenLeftHandedPreview() {
    SettingsScreenPreview(PreviewState.copy(leftHanded = true))
}

@Preview(widthDp = 360, heightDp = 2400, fontScale = 2f)
@Composable
internal fun SettingsScreenColorfulLargeFontPreview() {
    SettingsScreenPreview()
}

/**
 * Android removing the animations, without a vibrator, the sound effects off, the record just cleared: every greyed
 * setting, Effets réduits imposed, and « Record effacé ».
 */
@Preview(widthDp = 360, heightDp = 1200)
@Composable
internal fun SettingsScreenGreyedPreview() {
    SettingsScreenPreview(
        PreviewState.copy(soundEffects = false, canVibrate = false, recordClearing = RecordClearing.CLEARED),
        effects = BloxEffects(reduced = true, forcedBySystem = true),
    )
}

/** The tags of the cards of the page, for the tests. */
internal const val RECORD_CARD_TAG = "settings_record_card"
internal const val DISPLAY_CARD_TAG = "settings_display_card"
internal const val SOUNDS_CARD_TAG = "settings_sounds_card"
