package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.component.ChoiceGroup
import ch.magori.blox.core.designsystem.component.ChoiceItem
import ch.magori.blox.core.designsystem.component.LabelledControl
import ch.magori.blox.core.designsystem.component.LevelGauge
import ch.magori.blox.core.designsystem.component.ListenButton
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The card of the settings of the next game (V27): Mode, Thème, Niveau and Musique, one row each, 16 dp apart, in a
 * `panel` of `radius-window` framed by `panel-border`. Each choice is saved by the view model as soon as it is made.
 */
@Composable
internal fun HomeSettings(
    uiState: HomeUiState,
    onThemeChosen: (ThemeChoice) -> Unit,
    onStartLevelChosen: (Int) -> Unit,
    onMusicChosen: (MusicChoice) -> Unit,
    onListen: (MusicChoice) -> Unit,
    onListenGameMusic: () -> Unit,
    onChangeMusicFile: () -> Unit,
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
        // Until the sheet Musique (P5.76): the choices of P5.63, and one line of actions under them.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MusicChoices(uiState, onMusicChosen)
            MusicActions(uiState, onListen, onListenGameMusic, onChangeMusicFile)
        }
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
 * The music choices, on two columns. The music of the player played during the games, if any, stands before
 * « Aucune », the only music selected: shown by its name, on two lines if need be (a name of 40 characters, shrunk to
 * one line, could no longer be read), and read as the music of the games. Choosing it again does nothing. It stays
 * until another music is chosen, then leaves the home: the tile « Mes musiques » leads to it again.
 */
@Composable
private fun MusicChoices(
    uiState: HomeUiState,
    onMusicChosen: (MusicChoice) -> Unit,
) {
    val gameMusic = uiState.gameMusic
    // Each choice, and the music it chooses: none for the music of the player, already chosen.
    val musics: List<Pair<ChoiceItem, MusicChoice?>> = buildList {
        MusicChoice.entries.forEach { music ->
            if (music == MusicChoice.NONE && gameMusic != null) {
                val description = stringResource(R.string.feature_home_music_game_description, gameMusic.name)
                add(ChoiceItem(gameMusic.name, description = description, maxLines = 2) to null)
            }
            add(ChoiceItem(stringResource(music.label)) to music)
        }
    }
    val selected = if (gameMusic != null) {
        musics.indexOfFirst { (_, music) -> music == null }
    } else {
        musics.indexOfFirst { (_, music) -> music == uiState.selectedMusic }
    }
    ChoiceGroup(
        label = stringResource(R.string.feature_home_music),
        choices = musics.map { (choice, _) -> choice },
        selected = selected,
        onChosen = { index -> musics[index].second?.let(onMusicChosen) },
        columns = MUSIC_COLUMNS,
    )
}

/**
 * The line of actions under the music choices, for the music selected only: *Écouter* or *Arrêter* for a music that may
 * be listened to, the music of the player for the games included, and *Changer* for « Mon fichier » with a file kept.
 * Nothing for « Aucune ». The actions stand side by side, or one under the other in a large font.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusicActions(
    uiState: HomeUiState,
    onListen: (MusicChoice) -> Unit,
    onListenGameMusic: () -> Unit,
    onChangeMusicFile: () -> Unit,
) {
    val gameMusic = uiState.gameMusic
    val selected = uiState.selectedMusic
    FlowRow(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2, Alignment.CenterHorizontally)) {
        if (gameMusic != null) {
            ListenButton(playing = gameMusic.previewed, musicName = gameMusic.name, onClick = onListenGameMusic)
        } else if (selected != null) {
            if (selected in uiState.listenable) {
                ListenButton(
                    playing = selected == uiState.previewing,
                    musicName = stringResource(selected.label),
                    onClick = { onListen(selected) },
                )
            }
            if (selected == MusicChoice.MY_FILE && uiState.hasMusicFile) {
                ChangeMusicFileButton(onClick = onChangeMusicFile)
            }
        }
    }
}

/** The music choices side by side, on as many rows as they need. */
private const val MUSIC_COLUMNS = 2

@get:StringRes
private val ThemeChoice.label: Int
    get() = when (this) {
        ThemeChoice.ORIGINAL -> R.string.feature_home_theme_original
        ThemeChoice.COLORFUL -> R.string.feature_home_theme_colorful
    }

/** The name of a music choice, as on its segment and in the reminder under Jouer. */
@get:StringRes
internal val MusicChoice.label: Int
    get() = when (this) {
        MusicChoice.KOROBEINIKI -> R.string.feature_home_music_korobeiniki
        MusicChoice.MINUET -> R.string.feature_home_music_minuet
        MusicChoice.MY_FILE -> R.string.feature_home_music_my_file
        MusicChoice.NONE -> R.string.feature_home_music_none
    }
