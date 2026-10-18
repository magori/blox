package ch.magori.blox.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * Squelette de l'écran de jeu. Il sera branché sur un GameViewModel qui expose
 * un GameUiState issu de core:game (voir docs/architecture.md).
 */
@Composable
internal fun GameScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.page)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.feature_game_placeholder),
            color = palette.ink,
        )
        OutlinedButton(onClick = onBack) {
            Text(text = stringResource(R.string.feature_game_back))
        }
    }
}

@Preview
@Composable
private fun GameScreenPreview() {
    BloxTheme {
        GameScreen(onBack = {})
    }
}
