package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.text.scoreText
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** The smallest height of a choice, so that a finger can hit it. */
private val MinTouchHeight = 48.dp

/** The alpha of a choice that cannot be made yet. */
private const val DISABLED_ALPHA = 0.38f

/**
 * The home screen (V1): logo, title and record, mode, theme, start level, music, and the play button.
 * It is drawn in the theme of the application, which follows the chosen theme: a new choice colours
 * this screen at once. A music with a BloX score may be listened to before playing.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onThemeChosen: (ThemeChoice) -> Unit,
    onMusicChosen: (MusicChoice) -> Unit,
    onListen: (MusicChoice) -> Unit,
    onStartLevelChosen: (Int) -> Unit,
    onPlay: (startLevel: Int) -> Unit,
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
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Section(title = R.string.feature_home_music) {
            ChoiceRow {
                MusicChoice.entries.forEach { music ->
                    // A music with a BloX score may be listened to: its button stands under its choice.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val name = stringResource(music.label)
                        Choice(text = name, selected = music == uiState.music, onClick = { onMusicChosen(music) })
                        if (music in uiState.listenable) {
                            ListenButton(
                                musicName = name,
                                previewed = music == uiState.previewing,
                                onClick = { onListen(music) },
                            )
                        }
                    }
                }
            }
        }
        // Taps count only while this screen is resumed: once a game is on its way, a second tap does nothing.
        Button(onClick = dropUnlessResumed { onPlay(uiState.startLevel) }) {
            Text(text = stringResource(R.string.feature_home_play))
        }
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
 * A choice, read by TalkBack as a radio button. With [onClick] null, it is shown and announced as
 * selected, without an action: there is nothing else to choose.
 */
@Composable
private fun Choice(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = BloxTheme.palette
    val interaction = if (onClick != null) {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier.semantics {
            this.selected = selected
            role = Role.RadioButton
            if (!enabled) disabled()
        }
    }
    Box(
        modifier = modifier
            .heightIn(min = MinTouchHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(if (selected) palette.accent else palette.button, MaterialTheme.shapes.small)
            .then(interaction)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) palette.accentInk else palette.buttonInk,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

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

@Preview(widthDp = 360)
@Composable
private fun HomeScreenOriginalPreview() {
    HomeScreenPreview(BloxThemeKind.ORIGINAL, ThemeChoice.ORIGINAL)
}

@Preview(widthDp = 360)
@Composable
private fun HomeScreenColorfulPreview() {
    HomeScreenPreview(BloxThemeKind.COLORFUL, ThemeChoice.COLORFUL)
}

@Composable
private fun HomeScreenPreview(
    kind: BloxThemeKind,
    theme: ThemeChoice,
) {
    BloxTheme(kind = kind) {
        HomeScreen(
            uiState = HomeUiState(
                theme = theme,
                music = MusicChoice.KOROBEINIKI,
                startLevel = 0,
                startLevels = (0..9).toList(),
                record = 12_340,
                listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET),
                previewing = null,
            ),
            onThemeChosen = {},
            onMusicChosen = {},
            onListen = {},
            onStartLevelChosen = {},
            onPlay = {},
        )
    }
}
