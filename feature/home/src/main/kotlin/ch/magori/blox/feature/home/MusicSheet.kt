package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.designsystem.component.BloxSheet
import ch.magori.blox.core.designsystem.component.ListenButton
import ch.magori.blox.core.designsystem.component.SheetChoiceRow
import ch.magori.blox.core.designsystem.component.SheetChoices
import ch.magori.blox.core.designsystem.theme.BloxSpaces

/**
 * The sheet Musique of the home (V27, ADR 0029): a row of 56 dp per music the games may play, in the order of
 * [OFFERED_MUSICS], the music of the player for the games, if any, before « Aucune ». Each row is a choice, the one
 * selected checked; choosing one saves it ([MusicActions.onMusicChosen]) and leaves the sheet open. Each music that may
 * be listened to bears its listen button at the end of its row, one only playing at a time (the view model stops the
 * other); « Mon fichier » with a file kept bears Changer before it, which opens the picker. The system back or a touch
 * on the scrim calls [onDismissRequest].
 */
@Composable
internal fun MusicSheet(
    uiState: HomeUiState,
    music: MusicActions,
    onDismissRequest: () -> Unit,
) {
    BloxSheet(title = stringResource(R.string.feature_home_music), onDismissRequest = onDismissRequest) {
        MusicSheetChoices(uiState, music)
    }
}

/** What the sheet Musique holds: its choices, each on its row, as [MusicSheet] says. */
@Composable
internal fun MusicSheetChoices(
    uiState: HomeUiState,
    music: MusicActions,
) {
    SheetChoices {
        OFFERED_MUSICS.forEach { choice ->
            val gameMusic = uiState.gameMusic
            if (choice == MusicChoice.NONE && gameMusic != null) {
                MusicSheetRow(
                    choice = {
                        SheetChoiceRow(
                            label = gameMusic.name,
                            subLine = stringResource(R.string.feature_home_music_game_sub_line),
                            selected = true,
                            onClick = {},
                        )
                    },
                ) {
                    ListenButton(
                        playing = gameMusic.previewed,
                        musicName = gameMusic.name,
                        onClick = music.onListenGameMusic,
                    )
                }
            }
            ChoiceRow(choice, uiState, music)
        }
    }
}

/**
 * The row of [choice] in the sheet: the choice, then Changer for « Mon fichier » with a file, then Écouter; Changer goes
 * under them, at the end of the row, when beside them it would leave the choice too little room for its words.
 */
@Composable
private fun ChoiceRow(
    choice: MusicChoice,
    uiState: HomeUiState,
    music: MusicActions,
) {
    val name = stringResource(choice.label)
    MusicSheetRow(
        choice = {
            SheetChoiceRow(
                label = name,
                subLine = stringResource(choice.subLine(uiState.hasMusicFile)),
                selected = choice == uiState.selectedMusic,
                onClick = { music.onMusicChosen(choice) },
            )
        },
        extra = {
            if (choice == MusicChoice.MY_FILE && uiState.hasMusicFile) {
                ChangeMusicFileButton(onClick = music.onChangeMusicFile)
            }
        },
    ) {
        if (choice in uiState.listenable) {
            ListenButton(
                playing = choice == uiState.previewing,
                musicName = name,
                onClick = { music.onListen(choice) },
            )
        }
    }
}

/**
 * A row of the sheet: its [choice], which takes the room left, then its [extra] action, if any, then its [listen]
 * button, centred on the line. When the choice would get less room beside them than its longest word needs (a large
 * font), the extra action goes on a line of its own, under the row, at its end. No action is measured wider than the
 * row, nor placed before its start.
 */
@Composable
private fun MusicSheetRow(
    choice: @Composable () -> Unit,
    extra: @Composable () -> Unit = {},
    listen: @Composable () -> Unit,
) {
    Layout(contents = listOf(choice, extra, listen), modifier = Modifier.fillMaxWidth()) { parts, constraints ->
        val (choicePart, extraParts, listenParts) = parts
        val gap = BloxSpaces.space2.roundToPx()
        val width = constraints.maxWidth
        val placedListen = listenParts.map { it.measure(Constraints(maxWidth = width)) }
        val placedExtra = extraParts.map { it.measure(Constraints(maxWidth = width)) }
        val listenWidth = placedListen.sumOf { it.width + gap }
        val extraWidth = placedExtra.sumOf { it.width + gap }
        val chosen = choicePart.single()
        val beside = width - listenWidth - extraWidth >= chosen.minIntrinsicWidth(Constraints.Infinity)
        val choiceWidth = (width - listenWidth - if (beside) extraWidth else 0).coerceAtLeast(0)
        val placedChoice = chosen.measure(Constraints.fixedWidth(choiceWidth))
        val lineHeight = maxOf(placedChoice.height, placedListen.maxOfOrNull { it.height } ?: 0)
        val under = if (beside) 0 else placedExtra.maxOfOrNull { it.height } ?: 0
        val height = if (beside) maxOf(lineHeight, placedExtra.maxOfOrNull { it.height } ?: 0) else lineHeight + under
        layout(width, height) {
            val line = if (beside) height else lineHeight
            placedChoice.placeRelative(0, (line - placedChoice.height) / 2)
            var x = (choiceWidth + gap).coerceAtLeast(0)
            if (beside) {
                placedExtra.forEach {
                    it.placeRelative(x, (line - it.height) / 2)
                    x += it.width + gap
                }
            } else {
                placedExtra.forEach { it.placeRelative((width - it.width).coerceAtLeast(0), lineHeight) }
            }
            placedListen.forEach { it.placeRelative(x, (line - it.height) / 2) }
        }
    }
}

/** What a music choice is, under its name in the sheet; for « Mon fichier », whether a file is kept, [hasFile]. */
@StringRes
private fun MusicChoice.subLine(hasFile: Boolean): Int = when (this) {
    MusicChoice.KOROBEINIKI, MusicChoice.MINUET -> R.string.feature_home_music_public_domain

    MusicChoice.MY_FILE -> {
        if (hasFile) R.string.feature_home_music_my_file_kept else R.string.feature_home_music_my_file_pick
    }

    MusicChoice.LIVE -> R.string.feature_home_music_live_sub_line

    MusicChoice.NONE -> R.string.feature_home_music_none_sub_line
}
