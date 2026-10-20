package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.text.scoreText
import ch.magori.blox.core.designsystem.theme.BloxAlphas
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/** The tag of each start level, the same in every language, read by the tests. */
internal const val START_LEVEL_TAG = "home_start_level"

/** The tag of the logo, decorative, read by the tests only. */
internal const val HOME_LOGO_TAG = "home_logo"

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
        Section(title = R.string.feature_home_mode) {
            ChoiceRow {
                // The only mode for now: chosen, with nothing to do on it.
                Choice(text = stringResource(R.string.feature_home_mode_classic), selected = true, onClick = null)
                // Shown greyed out, and not clickable: the Modern mode comes later (P6).
                Choice(
                    text = stringResource(R.string.feature_home_mode_modern),
                    selected = false,
                    enabled = false,
                    onClick = {},
                )
            }
        }
        Section(title = R.string.feature_home_theme) {
            ChoiceRow {
                ThemeChoice.entries.forEach { theme ->
                    Choice(
                        text = stringResource(theme.label),
                        selected = theme == uiState.theme,
                        onClick = { onThemeChosen(theme) },
                    )
                }
            }
        }
        Section(title = R.string.feature_home_start_level) {
            // 0 to 4 above 5 to 9: on 360 dp, ten cells side by side would be 28 dp wide, five are 59 dp. One group of
            // radio buttons, read from 0 to 9: the rows only lay the cells out. Every row holds as many slots, so the
            // cells are all as wide, and a last row of fewer levels stands under the first cells of the row above.
            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(START_LEVEL_SPACING),
            ) {
                val rows = remember(uiState.startLevels) { uiState.startLevelRows }
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(START_LEVEL_SPACING)) {
                        row.forEach { level ->
                            Choice(
                                text = level.toString(),
                                selected = level == uiState.startLevel,
                                onClick = { onStartLevelChosen(level) },
                                checkInCorner = true,
                                modifier = Modifier.weight(1f).testTag(START_LEVEL_TAG),
                            )
                        }
                        repeat(START_LEVELS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Section(
            title = R.string.feature_home_music,
            action = { MyMusicsLink(onClick = dropUnlessResumed { onMyMusics() }) },
        ) {
            ChoiceRow {
                MusicChoice.entries.forEach { music ->
                    // The music of the player chosen for the games stands before « Aucune », as on the mockup.
                    if (music == MusicChoice.NONE) uiState.gameMusic?.let { GameMusicChoice(gameMusic = it) }
                    Choice(
                        text = stringResource(music.label),
                        selected = music == uiState.selectedMusic,
                        onClick = { onMusicChosen(music) },
                    )
                }
            }
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
                .testTag("home_play")
                .heightIn(min = BloxSizes.minTouchTarget),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.accentInk),
        ) {
            Text(text = stringResource(R.string.feature_home_play))
        }
    }
}

/**
 * The music of the player played during the games, [gameMusic], selected among the music choices: shown by its name,
 * and announced as selected, without an action. It stays until another music is chosen, then leaves the home screen:
 * « Mes musiques » chooses it again. Its score may be listened to, from the line of actions.
 */
@Composable
private fun GameMusicChoice(gameMusic: GameMusicUi) {
    // Its name on two lines if needed: a name of 40 characters, shrunk to one line, could no longer be read.
    Choice(
        text = gameMusic.name,
        selected = true,
        onClick = null,
        description = stringResource(R.string.feature_home_music_game_description, gameMusic.name),
        maxLines = 2,
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
        SettingsButton(onClick = onSettings)
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

/** A titled group of choices, with an [action] beside its title, if any. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(
    @StringRes title: Int,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // The action of the section beside its title, or under it when short of room.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(title),
                color = BloxTheme.palette.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            action?.invoke()
        }
        content()
    }
}

/** Choices of which one only is selected, wrapped on several lines when the screen is narrow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/**
 * A choice, read by TalkBack as a radio button. With [onClick] null, it is shown and announced as
 * selected, without an action: there is nothing else to choose. TalkBack reads [description] instead of [text], if
 * given. [text] takes one line, or up to [maxLines], shrinking if needed. A selected choice bears a check before its
 * label, or in its top corner if [checkInCorner].
 */
@Composable
private fun Choice(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    maxLines: Int = 1,
    checkInCorner: Boolean = false,
) {
    val colors = choiceColors(BloxTheme.palette, selected)
    // A choice is a button, framed as the secondary buttons of the game screens: square corners in Original.
    val shape = BloxTheme.shapes.button
    val interaction = if (onClick != null) {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    } else {
        // Merged with its text, as `selectable` does: TalkBack reads the choice and its state as one radio button.
        Modifier.semantics(mergeDescendants = true) {
            this.selected = selected
            role = Role.RadioButton
            if (!enabled) disabled()
        }
    }
    Box(
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .alpha(if (enabled) 1f else BloxAlphas.DISABLED)
            .panelFrame(colors, shape)
            .then(interaction)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val style = MaterialTheme.typography.labelLarge
        // With a description, TalkBack reads it alone, not the text after it.
        val readLabel = description == null
        if (checkInCorner) {
            // A start level: the check stands in its top corner, beside the digit rather than before it, so the digit
            // stays in the middle of its cell, under the digits of the other row. Every cell keeps that room on both
            // sides of its digit, selected or not, so the check never touches it, even in a large font, and the cells
            // stay as tall as the other choices: 48 dp at least.
            val room = CORNER_CHECK_INSET + CORNER_CHECK_SIZE
            ChoiceLabel(
                text = text,
                color = colors.ink,
                style = style,
                maxLines = maxLines,
                read = readLabel,
                modifier = Modifier.padding(horizontal = room),
            )
            colors.check?.let {
                ChoiceCheck(it, Modifier.align(Alignment.TopEnd).padding(CORNER_CHECK_INSET).size(CORNER_CHECK_SIZE))
            }
        } else {
            // Before the label, as large as its letters up to a limit, so it grows with the font, centred on the label,
            // even on two lines.
            val label = with(LocalDensity.current) { style.fontSize.toDp() }
            val insets = choiceInsets(selected = selected, label = label)
            Row(
                modifier = Modifier.padding(start = insets.start, end = insets.end),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                colors.check?.let {
                    ChoiceCheck(it, Modifier.size(insets.check))
                    Spacer(Modifier.width(insets.gap))
                }
                ChoiceLabel(text, colors.ink, style, maxLines, readLabel)
            }
        }
    }
}

/**
 * The label of a choice, [text] in [color], on one line or up to [maxLines], shrinking if needed: in a large font, on a
 * narrow phone, a label may be wider than its choice. TalkBack reads it if [read].
 */
@Composable
private fun ChoiceLabel(
    text: String,
    color: Color,
    style: TextStyle,
    maxLines: Int,
    read: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = if (read) modifier else modifier.clearAndSetSemantics {},
        color = color,
        style = style,
        textAlign = if (maxLines > 1) TextAlign.Center else null,
        overflow = if (maxLines > 1) TextOverflow.Ellipsis else TextOverflow.Clip,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        autoSize = shrinkToFit(style.fontSize),
    )
}

/** A cell of the logo, beside the title. */
private val LOGO_CELL_SIZE = 16.dp

/** The room between the logo and the title. */
private val HEADER_SPACING = 12.dp

/** The check of a start level, in its top corner: small, so that it fits beside the digit. */
private val CORNER_CHECK_SIZE = 10.dp

/** The room between the check of a start level and the edges of its cell, frame included. */
private val CORNER_CHECK_INSET = 4.dp

/** The room between two start level cells, side by side or one under the other. */
private val START_LEVEL_SPACING = 4.dp

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
