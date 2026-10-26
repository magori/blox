package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.component.Choice
import ch.magori.blox.core.designsystem.component.ChoiceGroup
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.text.scoreText
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/** The tag of the group of the start levels, the same in every language, read by the tests. */
internal const val START_LEVELS_TAG = "home_start_levels"

/** The tag of the logo, decorative, read by the tests only. */
internal const val HOME_LOGO_TAG = "home_logo"

/**
 * The tag of the play button, the same in every language, read by the tests and by the journey of `:benchmark`, which
 * copies it as it is: it does not see this module (`PlayTagTest`).
 */
internal const val PLAY_TAG = "home_play"

/**
 * The home screen (V1): logo and title on one line, the record under them, mode, theme, start level, music, and the
 * play button. It is drawn in the theme of the application, which follows the chosen theme: a new choice colours
 * this screen at once. A music with a BloX score may be listened to before playing. A music of the player chosen
 * for the games is the music selected, among the others. « Composer une musique » opens the music editor on a new
 * music, through [onCompose]; « Mes musiques », in the header of the music section, opens the list of the musics of
 * the player, through [onMyMusics]. The gear at the top right opens the settings page (V21), through [onSettings].
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onThemeChosen: (ThemeChoice) -> Unit,
    onMusicChosen: (MusicChoice) -> Unit,
    onListen: (MusicChoice) -> Unit,
    onListenGameMusic: () -> Unit,
    onChangeMusicFile: () -> Unit,
    onStartLevelChosen: (Int) -> Unit,
    onPlay: (startLevel: Int) -> Unit,
    onCompose: () -> Unit,
    onMyMusics: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(onSettings = dropUnlessResumed { onSettings() })
        Record(record = uiState.record)
        // The only mode for now, chosen, with nothing to do on it; the Modern mode comes later (P6): « BIENTÔT ».
        ChoiceGroup(
            label = stringResource(R.string.feature_home_mode),
            choices = listOf(
                Choice(stringResource(R.string.feature_home_mode_classic)),
                Choice(stringResource(R.string.feature_home_mode_modern), available = false),
            ),
            selected = 0,
            onChosen = {},
        )
        val themes = ThemeChoice.entries
        ChoiceGroup(
            label = stringResource(R.string.feature_home_theme),
            choices = themes.map { Choice(stringResource(it.label)) },
            selected = themes.indexOf(uiState.theme),
            onChosen = { onThemeChosen(themes[it]) },
        )
        // 0 to 4 above 5 to 9: on 360 dp, ten cells side by side would be 28 dp wide, five are 59 dp. One group of
        // radio buttons, read from 0 to 9: the rows only lay the cells out.
        val levels = uiState.startLevels
        ChoiceGroup(
            label = stringResource(R.string.feature_home_start_level),
            choices = levels.map { Choice(it.toString()) },
            selected = levels.indexOf(uiState.startLevel),
            onChosen = { onStartLevelChosen(levels[it]) },
            columns = START_LEVELS_PER_ROW,
            modifier = Modifier.testTag(START_LEVELS_TAG),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MusicChoices(uiState, onMusicChosen, onMyMusics = dropUnlessResumed { onMyMusics() })
            // One line of actions under all the choices, for the music selected only.
            MusicActions(
                uiState = uiState,
                onListen = onListen,
                onListenGameMusic = onListenGameMusic,
                onChangeMusicFile = onChangeMusicFile,
            )
            // Under the choice of music: opens the editor on a new music.
            ComposeMusicButton(onClick = dropUnlessResumed { onCompose() })
        }
        // Taps count only while this screen is resumed: once a game is on its way, a second tap does nothing.
        // The tag is read by the measured journey of `:benchmark` (`By.res`), whatever the language.
        // As tall as the choices: the button itself, not only the space Material keeps around it, is 48 dp high.
        // In the accent of the palette, as the main buttons of the game, whose contrast is tested (PaletteContrastTest).
        Button(
            onClick = dropUnlessResumed { onPlay(uiState.startLevel) },
            modifier = Modifier
                .testTag(PLAY_TAG)
                .heightIn(min = BloxSizes.minTouchTarget),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.accentInk),
        ) {
            // A triangle before the label, decorative: TalkBack reads « Jouer », nothing more.
            BloxIcon(BloxIcon.PLAY, Modifier.testTag(HOME_ICON_TAG))
            Spacer(Modifier.width(IconSizes.gap))
            Text(text = stringResource(R.string.feature_home_play))
        }
    }
}

/**
 * The music choices, on two columns, « Mes musiques » beside their label, which opens the musics of the player through
 * [onMyMusics]. The music of the player played during the games, if any, stands before « Aucune », as on the mockup,
 * the only music selected: shown by its name, on two lines if need be (a name of 40 characters, shrunk to one line,
 * could no longer be read), and read as the music of the games. Choosing it again does nothing. It stays until another
 * music is chosen, then leaves the home screen: « Mes musiques » chooses it again.
 */
@Composable
private fun MusicChoices(
    uiState: HomeUiState,
    onMusicChosen: (MusicChoice) -> Unit,
    onMyMusics: () -> Unit,
) {
    val gameMusic = uiState.gameMusic
    // Each choice, and the music it chooses: none for the music of the player, already chosen.
    val musics: List<Pair<Choice, MusicChoice?>> = buildList {
        MusicChoice.entries.forEach { music ->
            if (music == MusicChoice.NONE && gameMusic != null) {
                val description = stringResource(R.string.feature_home_music_game_description, gameMusic.name)
                add(Choice(gameMusic.name, description = description, maxLines = 2) to null)
            }
            add(Choice(stringResource(music.label)) to music)
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
        action = { MyMusicsLink(onClick = onMyMusics) },
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        if (gameMusic != null) {
            ListenButton(musicName = gameMusic.name, previewed = gameMusic.previewed, onClick = onListenGameMusic)
        } else if (selected != null) {
            if (selected in uiState.listenable) {
                ListenButton(
                    musicName = stringResource(selected.label),
                    previewed = selected == uiState.previewing,
                    onClick = { onListen(selected) },
                )
            }
            if (selected == MusicChoice.MY_FILE && uiState.hasMusicFile) {
                ChangeMusicFileButton(onClick = onChangeMusicFile)
            }
        }
    }
}

/**
 * The logo and the title on one line, centred on the screen, and the gear button at the top right, which opens the
 * settings through [onSettings]. Room as wide as the gear stands on the left, so that the title stays in the middle.
 * In a large font, the title shrinks rather than going on two lines.
 */
@Composable
private fun Header(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(BloxSizes.minTouchTarget))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(HEADER_SPACING, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeLogo(modifier = Modifier.testTag(HOME_LOGO_TAG), cellSize = LOGO_CELL_SIZE)
            val style = MaterialTheme.typography.displaySmall
            Text(
                text = stringResource(R.string.feature_home_title),
                color = BloxTheme.palette.ink,
                style = style,
                maxLines = 1,
                softWrap = false,
                autoSize = shrinkToFit(style.fontSize),
                modifier = Modifier.semantics { heading() },
            )
        }
        GlyphButton(
            icon = BloxIcon.GEAR,
            description = stringResource(R.string.feature_home_settings),
            onClick = onSettings,
        )
    }
}

/** The record, capped as on the game screen; TalkBack reads its real value. */
@Composable
private fun Record(record: Int) {
    val description = stringResource(R.string.feature_home_record_description, record)
    Text(
        text = stringResource(R.string.feature_home_record_value, scoreText(record)),
        color = BloxTheme.palette.ink,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    )
}

/** A cell of the logo, beside the title. */
private val LOGO_CELL_SIZE = 16.dp

/** The room between the logo and the title. */
private val HEADER_SPACING = 12.dp

/** The music choices side by side, on as many rows as they need. */
private const val MUSIC_COLUMNS = 2

@get:StringRes
private val ThemeChoice.label: Int
    get() = when (this) {
        ThemeChoice.ORIGINAL -> R.string.feature_home_theme_original
        ThemeChoice.COLORFUL -> R.string.feature_home_theme_colorful
    }

@get:StringRes
private val MusicChoice.label: Int
    get() = when (this) {
        MusicChoice.KOROBEINIKI -> R.string.feature_home_music_korobeiniki
        MusicChoice.MINUET -> R.string.feature_home_music_minuet
        MusicChoice.MY_FILE -> R.string.feature_home_music_my_file
        MusicChoice.NONE -> R.string.feature_home_music_none
    }

@Composable
private fun HomeScreenPreview(
    theme: ThemeChoice,
    gameMusic: GameMusicUi? = null,
) {
    val kind = when (theme) {
        ThemeChoice.ORIGINAL -> BloxThemeKind.ORIGINAL
        ThemeChoice.COLORFUL -> BloxThemeKind.COLORFUL
    }
    BloxTheme(kind = kind) {
        HomeScreen(
            uiState = HomeUiState(
                theme = theme,
                music = MusicChoice.KOROBEINIKI,
                startLevel = 0,
                startLevels = (0..9).toList(),
                record = 12_340,
                // A music file kept: « Mon fichier » shows its listen and change buttons.
                listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET, MusicChoice.MY_FILE),
                previewing = null,
                hasMusicFile = true,
                gameMusic = gameMusic,
            ),
            onThemeChosen = {},
            onMusicChosen = {},
            onListen = {},
            onListenGameMusic = {},
            onChangeMusicFile = {},
            onStartLevelChosen = {},
            onPlay = {},
            onCompose = {},
            onMyMusics = {},
            onSettings = {},
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun HomeScreenOriginalPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun HomeScreenColorfulPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 1300, fontScale = 2f)
@Composable
internal fun HomeScreenLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 1300, fontScale = 2f)
@Composable
internal fun HomeScreenColorfulLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 880)
@Composable
internal fun HomeScreenGameMusicPreview() {
    // A music of the player chosen for the games: the only music selected, which may be listened to.
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = "Thème du soir", previewed = false))
}

/** A name of the most characters a saved music may have (SavedMusic.MAX_NAME_LENGTH). */
private const val LONGEST_NAME = "Ballades pour une longue soirée en hiver"

@Preview(widthDp = 360, heightDp = 880)
@Composable
internal fun HomeScreenGameMusicLongNamePreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = LONGEST_NAME, previewed = false))
}

@Preview(widthDp = 360, heightDp = 1380, fontScale = 2f)
@Composable
internal fun HomeScreenGameMusicLongNameLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = LONGEST_NAME, previewed = false))
}
