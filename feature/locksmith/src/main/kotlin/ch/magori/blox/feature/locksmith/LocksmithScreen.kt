package ch.magori.blox.feature.locksmith

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/**
 * The screen of the Locksmith (V35), drawn from [state] alone, always in the Colorful theme, day or night as the
 * system says (decision of the developer, 2026-10-26: the Locksmith has no Original look). Its padlock and its
 * controls come with P7.12.
 */
@Composable
internal fun LocksmithScreen(
    state: LocksmithUiState,
    modifier: Modifier = Modifier,
) {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BloxTheme.palette.page)
                .safeDrawingPadding()
                .padding(BloxSpaces.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.feature_locksmith_title, state.level),
                modifier = Modifier.semantics { heading() },
                color = BloxTheme.palette.ink,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
