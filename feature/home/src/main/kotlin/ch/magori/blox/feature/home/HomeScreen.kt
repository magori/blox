package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxTypographies

/** The tag of each start level, the same in every language, read by the tests. */
internal const val START_LEVEL_TAG = "home_start_level"

/**
 * The home screen (V1): logo, title and record, mode, theme, start level, music, and the play button.
 * It is drawn in the theme of the application, which follows the chosen theme: a new choice colours
 * this screen at once. A music with a BloX score may be listened to before playing. A music of the player chosen
 * for the games is the music selected, among the others. « Composer une musique » opens the music editor on a new
 * music, through [onCompose].
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
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeLogo()
        Text(
            text = stringResource(R.string.feature_home_title),
            color = palette.ink,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.semantics { heading() },
        )
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
            // 0 to 9 on a single line, as on the mockup: equal cells, as tall as a finger needs.
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                uiState.startLevels.forEach { level ->
                    Choice(
                        text = level.toString(),
                        selected = level == uiState.startLevel,
                        onClick = { onStartLevelChosen(level) },
                        checkInCorner = true,
                        modifier = Modifier.weight(1f).testTag(START_LEVEL_TAG),
                    )
                }
            }
        }
        Section(title = R.string.feature_home_music) {
            ChoiceRow {
                MusicChoice.entries.forEach { music ->
                    // The music of the player chosen for the games stands before « Aucune », as on the mockup.
                    if (music == MusicChoice.NONE) {
                        uiState.gameMusic?.let { GameMusicChoice(gameMusic = it, onListen = onListenGameMusic) }
                    }
                    // A music with a BloX score may be listened to: its button stands under its choice.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val name = stringResource(music.label)
                        val selected = music == uiState.selectedMusic
                        Choice(text = name, selected = selected, onClick = { onMusicChosen(music) })
                        // Beside it, or under it when short of room, « Mon fichier » with a file offers to change it.
                        ChoiceActions {
                            if (music in uiState.listenable) {
                                ListenButton(
                                    musicName = name,
                                    previewed = music == uiState.previewing,
                                    onClick = { onListen(music) },
                                )
                            }
                            if (music == MusicChoice.MY_FILE && uiState.hasMusicFile) {
                                ChangeMusicFileButton(onClick = onChangeMusicFile)
                            }
                        }
                    }
                }
            }
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
 * « Mes musiques » chooses it again. Its score may be listened to.
 */
@Composable
private fun GameMusicChoice(
    gameMusic: GameMusicUi,
    onListen: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Its name on two lines if needed: a name of 40 characters, shrunk to one line, could no longer be read.
        Choice(
            text = gameMusic.name,
            selected = true,
            onClick = null,
            description = stringResource(R.string.feature_home_music_game_description, gameMusic.name),
            maxLines = 2,
        )
        ListenButton(musicName = gameMusic.name, previewed = gameMusic.previewed, onClick = onListen)
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

/** A titled group of choices. */
@Composable
private fun Section(
    @StringRes title: Int,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(title),
            color = BloxTheme.palette.label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() },
        )
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
 * The actions under a choice, side by side, or one under the other when they do not fit (a large font): a label is
 * never cut in the middle of a word.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceActions(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.Center) { content() }
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
            .background(colors.background, shape)
            .border(BloxBorders.frame, colors.frame, shape)
            .then(interaction)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val style = MaterialTheme.typography.labelLarge
        // With a description, TalkBack reads it alone, not the text after it.
        val readLabel = description == null
        if (checkInCorner) {
            // A cell too narrow to give room to a check beside its digit: the check stands in its top corner, above
            // the digit. Every cell keeps that room above and under its digit, selected or not, so the check never
            // touches it, even in a large font, and the cells stay as tall as one another (48 dp in a normal font).
            val room = CORNER_CHECK_INSET + CORNER_CHECK_SIZE
            ChoiceLabel(
                text = text,
                color = colors.ink,
                style = style,
                maxLines = maxLines,
                read = readLabel,
                modifier = Modifier.padding(horizontal = CHOICE_PADDING, vertical = room),
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
 * The label of a choice, [text] in [color], on one line or up to [maxLines], shrinking if needed: a start level cell
 * is narrower than a digit of the pixel font. TalkBack reads it if [read].
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
        autoSize = TextAutoSize.StepBased(
            minFontSize = BloxTypographies.MinimumFontSize,
            maxFontSize = style.fontSize,
        ),
    )
}

/** The check of a start level, in its top corner: small, as the cell is narrow. */
private val CORNER_CHECK_SIZE = 10.dp

/** The room between the check of a start level and the edges of its cell, frame included. */
private val CORNER_CHECK_INSET = 4.dp

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
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360)
@Composable
internal fun HomeScreenOriginalPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL)
}

@Preview(widthDp = 360)
@Composable
internal fun HomeScreenColorfulPreview() {
    HomeScreenPreview(ThemeChoice.COLORFUL)
}

@Preview(widthDp = 360, fontScale = 2f)
@Composable
internal fun HomeScreenLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL)
}

@Preview(widthDp = 360)
@Composable
internal fun HomeScreenGameMusicPreview() {
    // A music of the player chosen for the games: the only music selected, which may be listened to.
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = "Thème du soir", previewed = false))
}

/** A name of the most characters a saved music may have (SavedMusic.MAX_NAME_LENGTH). */
private const val LONGEST_NAME = "Ballades pour une longue soirée en hiver"

@Preview(widthDp = 360, heightDp = 1000)
@Composable
internal fun HomeScreenGameMusicLongNamePreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = LONGEST_NAME, previewed = false))
}

@Preview(widthDp = 360, heightDp = 1450, fontScale = 2f)
@Composable
internal fun HomeScreenGameMusicLongNameLargeFontPreview() {
    HomeScreenPreview(ThemeChoice.ORIGINAL, gameMusic = GameMusicUi(name = LONGEST_NAME, previewed = false))
}
