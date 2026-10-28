package ch.magori.blox.feature.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords

/** What the player does on the musical garden, handed to `app` by the entry. */
@Immutable
internal class GardenActions(val onChangeGame: () -> Unit) {
    companion object {
        /** Actions that do nothing, for the previews. */
        val None = GardenActions(onChangeGame = {})
    }
}

/**
 * The musical garden (V40), its skeleton: the bar, with « Changer de jeu » and the title « JARDIN MUSICAL ». Drawn in
 * the Colorful theme only, day or night as the phone, the effects of the application kept (ADR 0026, ADR 0027). No
 * logic: what each action means belongs to the view model and to `app`. Changer de jeu counts only while the garden is
 * resumed: once the cartridges are on their way, a second tap does nothing.
 */
@Composable
internal fun GardenScreen(
    // Read by the grid, P10.10.
    uiState: GardenUiState,
    actions: GardenActions,
    modifier: Modifier = Modifier,
) {
    val changeGame = dropUnlessResumed { actions.onChangeGame() }
    ColorfulTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BloxTheme.palette.page)
                .safeDrawingPadding()
                .padding(BloxSpaces.space4),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
        ) {
            GardenBar(onChangeGame = changeGame)
        }
    }
}

/** The bar: the disc « Changer de jeu » at the start, then the title, in capitals, read as a heading in words. */
@Composable
private fun GardenBar(onChangeGame: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            icon = BloxIcon.CARTRIDGE,
            description = stringResource(R.string.feature_garden_change_game),
            onClick = onChangeGame,
        )
        val title = stringResource(R.string.feature_garden_title)
        Text(
            text = title.uppercase(),
            modifier = Modifier
                .weight(1f)
                // Its text kept, TalkBack reads the title in words in its place, as a heading.
                .semantics {
                    contentDescription = title
                    heading()
                },
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.gameTitle,
            // With a large font on a narrow screen, it shrinks rather than cut a word.
            autoSize = shrinkToWholeWords(BloxTheme.typography.gameTitle.fontSize),
        )
    }
}
