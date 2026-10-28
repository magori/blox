package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonEmphasis
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.ButtonSize
import ch.magori.blox.core.designsystem.component.ChoiceGroup
import ch.magori.blox.core.designsystem.component.ChoiceItem
import ch.magori.blox.core.designsystem.component.LabelledControl
import ch.magori.blox.core.designsystem.component.LevelGauge
import ch.magori.blox.core.designsystem.component.ListenButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The tag of the note struck through of « Aucune », decorative, read by the tests only. */
internal const val MUSIC_NONE_TAG = "music_none"

/** The tag of the slot of the listen button, left empty when there is nothing to listen to, read by the tests only. */
internal const val MUSIC_SLOT_TAG = "music_slot"

/**
 * The card of the settings of the next game (V27): Mode, Thème, Niveau and Musique, one row each, 16 dp apart, in a
 * `panel` of `radius-window` framed by `panel-border`. Each choice is saved by the view model as soon as it is made;
 * the row Musique opens the sheet Musique through [onOpenMusicSheet].
 */
@Composable
internal fun HomeSettings(
    uiState: HomeUiState,
    onThemeChosen: (ThemeChoice) -> Unit,
    onStartLevelChosen: (Int) -> Unit,
    music: MusicActions,
    onOpenMusicSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.window
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(BloxSpaces.space4),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
    ) {
        ModeRow()
        ThemeRow(uiState.theme, onThemeChosen)
        LevelRow(uiState.startLevel, uiState.startLevels, onStartLevelChosen)
        MusicRow(uiState, music, onOpenMusicSheet)
    }
}

/** The only mode for now, chosen, with nothing to do on it; the Modern mode comes later (P6): « BIENTÔT ». */
@Composable
private fun ModeRow() {
    ChoiceGroup(
        label = stringResource(R.string.feature_home_mode),
        choices = listOf(
            ChoiceItem(stringResource(R.string.feature_home_mode_classic)),
            ChoiceItem(stringResource(R.string.feature_home_mode_modern), available = false),
        ),
        selected = 0,
        onChosen = {},
    )
}

/** The themes, [theme] chosen; a new choice colours the home at once. */
@Composable
private fun ThemeRow(
    theme: ThemeChoice,
    onThemeChosen: (ThemeChoice) -> Unit,
) {
    val themes = ThemeChoice.entries
    ChoiceGroup(
        label = stringResource(R.string.feature_home_theme),
        choices = themes.map { ChoiceItem(stringResource(it.label)) },
        selected = themes.indexOf(theme),
        onChosen = { onThemeChosen(themes[it]) },
    )
}

/** The gauge of the start level, [level] of [levels], read « Niveau de départ ». */
@Composable
private fun LevelRow(
    level: Int,
    levels: IntRange,
    onStartLevelChosen: (Int) -> Unit,
) {
    LabelledControl(label = stringResource(R.string.feature_home_level)) {
        LevelGauge(
            level = level,
            levels = levels,
            onLevelChange = onStartLevelChosen,
            description = stringResource(R.string.feature_home_start_level),
        )
    }
}

/**
 * The row Musique: the listen button of the music selected, if it may be listened to (the music of the player for the
 * games included), for « Aucune » a note struck through, which nothing reads, or its room left empty (« Mon fichier »
 * without a file); then the name of the music chosen, which opens the sheet Musique through [onOpenMusicSheet]. The
 * reminder under Jouer, not this row, names the music played in place of a file missing.
 */
@Composable
private fun MusicRow(
    uiState: HomeUiState,
    music: MusicActions,
    onOpenMusicSheet: () -> Unit,
) {
    val gameMusic = uiState.gameMusic
    val selected = uiState.selectedMusic
    val name = gameMusic?.name ?: stringResource(uiState.music.label)
    LabelledControl(label = stringResource(R.string.feature_home_music)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        ) {
            when {
                gameMusic != null -> {
                    ListenButton(playing = gameMusic.previewed, musicName = name, onClick = music.onListenGameMusic)
                }

                selected != null && selected in uiState.listenable -> {
                    ListenButton(
                        playing = selected == uiState.previewing,
                        musicName = name,
                        onClick = { music.onListen(selected) },
                    )
                }

                selected == MusicChoice.NONE -> {
                    MusicSlot {
                        BloxIcon(
                            icon = BloxIcon.NOTE_OFF,
                            modifier = Modifier.testTag(MUSIC_NONE_TAG),
                            size = IconSizes.alone,
                            tint = BloxTheme.palette.label,
                        )
                    }
                }

                // « Mon fichier » without a file: nothing to listen to, its room kept so that the name stays in place.
                else -> MusicSlot(Modifier.testTag(MUSIC_SLOT_TAG)) {}
            }
            MusicSheetOpener(name, onOpenMusicSheet)
        }
    }
}

/**
 * The room of the listen button of the row Musique, as large, holding [content] in its centre: the note struck through
 * of « Aucune », or nothing. Nothing reads it.
 */
@Composable
private fun MusicSlot(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(BloxSizes.minTouchTarget), contentAlignment = Alignment.Center) { content() }
}

/**
 * The name of the music selected, [name], and a chevron down: a bare button, which opens the sheet Musique through
 * [onClick]. TalkBack reads it « Musique : [name]. Choisir une autre musique ».
 */
@Composable
private fun MusicSheetOpener(
    name: String,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.feature_home_music_open, name)
    BubbleButton(
        family = ButtonFamily.SECONDARY,
        onClick = onClick,
        shape = BloxTheme.shapes.md,
        emphasis = ButtonEmphasis.BARE,
        size = ButtonSize.COMPACT,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        BloxIcon(BloxIcon.CHEVRON_DOWN)
    }
}

/**
 * The music choices the sheet offers, in their order. « Vivante » waits for its row of the sheet (P8.7): until the game
 * composes its live music (P8.6), a live game would play nothing.
 */
internal val OFFERED_MUSICS: List<MusicChoice> = MusicChoice.entries - MusicChoice.LIVE

@get:StringRes
private val ThemeChoice.label: Int
    get() = when (this) {
        ThemeChoice.ORIGINAL -> R.string.feature_home_theme_original
        ThemeChoice.COLORFUL -> R.string.feature_home_theme_colorful
    }

/** The name of a music choice, as in the sheet Musique, on its row of the card and in the reminder under Jouer. */
@get:StringRes
internal val MusicChoice.label: Int
    get() = when (this) {
        MusicChoice.KOROBEINIKI -> R.string.feature_home_music_korobeiniki
        MusicChoice.MINUET -> R.string.feature_home_music_minuet
        MusicChoice.MY_FILE -> R.string.feature_home_music_my_file
        MusicChoice.LIVE -> R.string.feature_home_music_live
        MusicChoice.NONE -> R.string.feature_home_music_none
    }
