package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/** The tag of the logo, decorative, read by the tests only. */
internal const val HOME_LOGO_TAG = "home_logo"

/**
 * The home screen (V27, Couleurs v2), its parts laid out for a phone held upright ([HomeColumn]): the bar (Changer de
 * jeu, through [onChangeGame], the record, and the gear, which opens the settings page through [onSettings]), the logo
 * and the title, Jouer and the reminder of the game it starts, the card of the settings (mode, theme, start level,
 * music, each chosen saved by the view model), and the tiles « Mes musiques », through [onMyMusics], and « Composer une
 * musique », through [onCompose]. It is drawn in the theme of the application, which follows the chosen theme: a new
 * choice colours this screen at once. Taps on what leaves the home count only while it is resumed: once a game is on
 * its way, a second tap does nothing.
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
    onChangeGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changeGame = dropUnlessResumed { onChangeGame() }
    val openSettings = dropUnlessResumed { onSettings() }
    val play = dropUnlessResumed { onPlay(uiState.startLevel) }
    val openMyMusics = dropUnlessResumed { onMyMusics() }
    val compose = dropUnlessResumed { onCompose() }
    val mode = stringResource(R.string.feature_home_mode_classic)
    val music = uiState.gameMusic?.name ?: stringResource(uiState.music.label)
    val reminder = stringResource(R.string.feature_home_play_reminder, mode, uiState.startLevel, music)
    val playDescription = stringResource(R.string.feature_home_play_description, mode, uiState.startLevel, music)
    HomeColumn(
        modifier = modifier,
        parts = HomeParts(
            bar = { HomeBar(record = uiState.record, onChangeGame = changeGame, onSettings = openSettings) },
            hero = { HomeTitle() },
            play = { PlayButton(reminder = reminder, description = playDescription, onClick = play) },
            settings = {
                HomeSettings(
                    uiState = uiState,
                    onThemeChosen = onThemeChosen,
                    onStartLevelChosen = onStartLevelChosen,
                    onMusicChosen = onMusicChosen,
                    onListen = onListen,
                    onListenGameMusic = onListenGameMusic,
                    onChangeMusicFile = onChangeMusicFile,
                )
            },
            tiles = { HomeTiles(onMyMusics = openMyMusics, onCompose = compose) },
        ),
    )
}

/**
 * The logo and the title on one line, centred, until the hero of P5.77 (logo v2 and preview board). In a large font,
 * the title shrinks rather than going on two lines.
 */
@Composable
private fun HomeTitle() {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
}

/** A cell of the logo, beside the title. */
private val LOGO_CELL_SIZE = 16.dp

/** The room between the logo and the title. */
private val HEADER_SPACING = 12.dp

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
                startLevel = 5,
                startLevels = 0..9,
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
            onChangeGame = {},
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 1000)
@Composable
internal fun HomeScreenOriginalPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 1000)
@Composable
internal fun HomeScreenColorfulPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 1700, fontScale = 2f)
@Composable
internal fun HomeScreenLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 1700, fontScale = 2f)
@Composable
internal fun HomeScreenColorfulLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 1080)
@Composable
internal fun HomeScreenGameMusicPreview() {
    // A music of the player chosen for the games: the only music selected, which may be listened to.
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = "Thème du soir", previewed = false))
}

/** A name of the most characters a saved music may have (SavedMusic.MAX_NAME_LENGTH). */
private const val LONGEST_NAME = "Ballades pour une longue soirée en hiver"

@Preview(widthDp = 360, heightDp = 1080)
@Composable
internal fun HomeScreenGameMusicLongNamePreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = LONGEST_NAME, previewed = false))
}

@Preview(widthDp = 360, heightDp = 1780, fontScale = 2f)
@Composable
internal fun HomeScreenGameMusicLongNameLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = LONGEST_NAME, previewed = false))
}
