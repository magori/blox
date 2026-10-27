package ch.magori.blox.feature.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.component.BloxSheetPreview
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.shrinkToFit
import ch.magori.blox.core.game.model.Level

/** The tag of the preview board, decorative, read by the tests only. */
internal const val HOME_PREVIEW_TAG = "home_preview"

/**
 * The home screen (V27, Couleurs v2), its parts laid out for a phone held upright ([HomeColumn]): the bar (Changer de
 * jeu, through [onChangeGame], the record, and the gear, which opens the settings page through [onSettings]), the hero
 * ([HomeHero]: logo and preview), Jouer with its halo and the reminder of the game it starts, the card of the settings
 * (mode, theme, start level, music, each chosen saved by the view model), and the tiles « Mes musiques », through
 * [onMyMusics], and « Composer une musique », through [onCompose]. The row Musique opens the sheet Musique
 * ([MusicSheet]), which the screen holds open across a rotation (ADR 0029); closing it tells [music]. It is drawn in
 * the theme of the application, which follows the chosen theme: a new choice colours this screen at once. Taps on what
 * leaves the home count only while it is resumed: once a game is on its way, a second tap does nothing.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onThemeChosen: (ThemeChoice) -> Unit,
    music: MusicActions,
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
    var musicSheetOpen by rememberSaveable { mutableStateOf(false) }
    val mode = stringResource(R.string.feature_home_mode_classic)
    val musicName = uiState.gameMusic?.name ?: stringResource(uiState.remindedMusic.label)
    val reminder = stringResource(R.string.feature_home_play_reminder, mode, uiState.startLevel, musicName)
    val playDescription = stringResource(R.string.feature_home_play_description, mode, uiState.startLevel, musicName)
    // Original keeps its hero of v1, still; in Colorful, the preview and the halo move, unless the effects are reduced.
    val original = BloxTheme.variant == BloxThemeVariant.ORIGINAL
    val still = BloxTheme.effects.reduced
    // A preview of Android Studio, and its capture, shows the T as it comes in: a still and certain instant.
    val inspected = LocalInspectionMode.current
    val level = Level(uiState.startLevel)
    val motion = rememberHomeMotion(level, plays = !original && !still && !inspected)
    val halo = remember(motion) { { motion.halo } }
    HomeColumn(
        modifier = modifier.touched(motion),
        parts = HomeParts(
            bar = { HomeBar(record = uiState.record, onChangeGame = changeGame, onSettings = openSettings) },
            hero = { if (original) HomeTitle() else HomeHero(level, motion, still) },
            play = { PlayButton(reminder = reminder, description = playDescription, onClick = play, halo = halo) },
            settings = {
                HomeSettings(
                    uiState = uiState,
                    onThemeChosen = onThemeChosen,
                    onStartLevelChosen = onStartLevelChosen,
                    music = music,
                    onOpenMusicSheet = { musicSheetOpen = true },
                )
            },
            tiles = { HomeTiles(onMyMusics = openMyMusics, onCompose = compose) },
        ),
    )
    if (musicSheetOpen) {
        MusicSheet(
            uiState = uiState,
            music = music,
            onDismissRequest = {
                musicSheetOpen = false
                music.onSheetClosed()
            },
        )
    }
}

/**
 * The hero of Original, as in v1: the logo of four cells and the title on one line, centred (P5.77 leaves it so). In a
 * large font, the title shrinks rather than going on two lines.
 */
@Composable
private fun HomeTitle() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HEADER_SPACING, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeLogo(cellSize = LOGO_CELL_SIZE)
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

/** Each touch on the home, wherever it lands, restarts the wait of the halo of Jouer; the touch goes on unchanged. */
private fun Modifier.touched(motion: HomeMotion): Modifier = pointerInput(motion) {
    awaitEachGesture {
        awaitPointerEvent(PointerEventPass.Initial)
        motion.touched()
    }
}

/** A cell of the logo, beside the title. */
private val LOGO_CELL_SIZE = 16.dp

/** The room between the logo and the title. */
private val HEADER_SPACING = 12.dp

/**
 * The home in [theme], Korobeïniki chosen, a music file kept, [gameMusic] if any; with [sheet], the sheet Musique open
 * over it, come in, the minuet listened to in it (a preview draws no window: the sheet is drawn over the screen).
 */
@Composable
private fun HomeScreenPreview(
    theme: ThemeChoice,
    gameMusic: GameMusicUi? = null,
    sheet: Boolean = false,
) {
    val kind = when (theme) {
        ThemeChoice.ORIGINAL -> BloxThemeKind.ORIGINAL
        ThemeChoice.COLORFUL -> BloxThemeKind.COLORFUL
    }
    val uiState = HomeUiState(
        theme = theme,
        music = MusicChoice.KOROBEINIKI,
        startLevel = 5,
        startLevels = 0..9,
        record = 12_340,
        // A music file kept: « Mon fichier » may be listened to, and changed in the sheet.
        listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET, MusicChoice.MY_FILE),
        previewing = if (sheet) MusicChoice.MINUET else null,
        hasMusicFile = true,
        gameMusic = gameMusic,
    )
    BloxTheme(kind = kind) {
        HomeScreen(
            uiState = uiState,
            onThemeChosen = {},
            music = MusicActions.None,
            onStartLevelChosen = {},
            onPlay = {},
            onCompose = {},
            onMyMusics = {},
            onSettings = {},
            onChangeGame = {},
        )
        if (sheet) {
            BloxSheetPreview(title = stringResource(R.string.feature_home_music)) {
                MusicSheetChoices(uiState, MusicActions.None)
            }
        }
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

@Preview(widthDp = 360, heightDp = 1000)
@Composable
internal fun HomeMusicSheetColorfulPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL, sheet = true)
}

@Preview(widthDp = 360, heightDp = 1000)
@Composable
internal fun HomeMusicSheetOriginalPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL, sheet = true)
}

@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
internal fun HomeMusicSheetColorfulLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL, sheet = true)
}

@Preview(widthDp = 360, heightDp = 1000)
@Composable
internal fun HomeMusicSheetGameMusicPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL, GameMusicUi(name = "Thème du soir", previewed = false), sheet = true)
}
