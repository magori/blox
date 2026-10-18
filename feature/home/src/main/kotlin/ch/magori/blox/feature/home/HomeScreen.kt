package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * Squelette de l'écran d'accueil. Contenu cible (mode, thème, niveau, musique) :
 * voir docs/maquette.md et la tâche correspondante dans SUIVI.md.
 */
@Composable
internal fun HomeScreen(
    onPlay: () -> Unit,
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
            text = stringResource(R.string.feature_home_title),
            color = palette.ink,
            style = MaterialTheme.typography.displayMedium,
        )
        // Taps count only while this screen is resumed: once a game is on its way, a second tap does nothing.
        Button(onClick = dropUnlessResumed(block = onPlay)) {
            Text(text = stringResource(R.string.feature_home_play))
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    BloxTheme {
        HomeScreen(onPlay = {})
    }
}
