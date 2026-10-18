package ch.magori.blox.feature.game.pause

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.PrimaryButton
import ch.magori.blox.feature.game.ui.SecondaryButton
import ch.magori.blox.feature.game.ui.TitledScreen

/**
 * The pause screen: the title, and the buttons to resume or go back to the menu. It covers the
 * whole game screen with the page background; the board is masked while paused, as the pause must
 * not be a time to think (docs/regles-mode-classique.md, Commandes). TalkBack announces it by its
 * pane title. No logic here: forgetting the held keys and giving no tick belong to the engine and
 * the ViewModel.
 */
@Composable
internal fun PauseScreen(
    onResume: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TitledScreen(title = stringResource(R.string.feature_game_pause_title), modifier = modifier) {
        PrimaryButton(text = stringResource(R.string.feature_game_pause_resume), onClick = onResume)
        SecondaryButton(text = stringResource(R.string.feature_game_pause_menu), onClick = onMenu)
    }
}

@Composable
private fun PausePreview(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        PauseScreen(onResume = {}, onMenu = {})
    }
}

@Preview
@Composable
private fun PauseOriginalPreview() {
    PausePreview(BloxThemeKind.ORIGINAL)
}

@Preview
@Composable
private fun PauseColorfulPreview() {
    PausePreview(BloxThemeKind.COLORFUL)
}

@Preview(fontScale = 2f)
@Composable
private fun PauseLargeFontPreview() {
    PausePreview(BloxThemeKind.ORIGINAL)
}
