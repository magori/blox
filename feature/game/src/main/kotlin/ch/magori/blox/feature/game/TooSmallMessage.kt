package ch.magori.blox.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords
import ch.magori.blox.core.designsystem.theme.shrinkingLineHeight

/**
 * « Agrandissez la fenêtre », centred in the space of the board, in the ink of the theme on its page, in the font of
 * the titles. At 14 sp, smaller only when a word would not fit whole: in the narrowest multi-window, 220 dp, the
 * widest word in the widest font, « Agrandissez » in Press Start 2P, fits whole even with the font twice as large.
 * TalkBack announces it as it comes (a polite live region).
 */
@Composable
internal fun TooSmallMessage(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val style = MaterialTheme.typography.titleSmall.shrinkingLineHeight()
        Text(
            text = stringResource(R.string.feature_game_too_small),
            color = BloxTheme.palette.ink,
            style = style,
            textAlign = TextAlign.Center,
            autoSize = shrinkToWholeWords(style.fontSize),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
