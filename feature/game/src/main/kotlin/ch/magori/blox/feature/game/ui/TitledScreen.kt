package ch.magori.blox.feature.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The sizes shared by the screens shown over the game (game over, pause), in one place. */
internal object ScreenSizes {
    /** Around the screen. */
    val padding = 24.dp

    /** Between two blocks of the screen. */
    val gap = 16.dp
    val title = 22.sp
}

/**
 * A screen shown over the game: the page background, its [title] as a heading, then its [content],
 * centred. TalkBack announces it by its pane title; it scrolls when the font is enlarged.
 */
@Composable
internal fun TitledScreen(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = title }
            .background(palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(ScreenSizes.padding),
        verticalArrangement = Arrangement.spacedBy(ScreenSizes.gap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = palette.ink,
            fontSize = ScreenSizes.title,
            style = BloxTheme.typography.title,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}
