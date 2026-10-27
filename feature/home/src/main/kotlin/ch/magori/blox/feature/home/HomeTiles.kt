package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.SideBySideOrStacked
import ch.magori.blox.core.designsystem.component.buttonLabelStyle
import ch.magori.blox.core.designsystem.component.sidePadding
import ch.magori.blox.core.designsystem.component.widestWord
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords

/** The tag of the icons of the home screen beside a label, decorative, read by the tests only. */
internal const val HOME_ICON_TAG = "home_icon"

/**
 * The tiles under the settings: « Mes musiques », which opens the musics of the player through [onMyMusics], and
 * « Composer une musique », which opens the editor on a new music through [onCompose]. Side by side and as high as each
 * other while the widest word of each label holds in its tile; one above the other otherwise, as in a large font.
 */
@Composable
internal fun HomeTiles(
    onMyMusics: () -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val myMusics = stringResource(R.string.feature_home_my_musics)
    val compose = stringResource(R.string.feature_home_compose)
    val measurer = rememberTextMeasurer()
    val style = buttonLabelStyle(ButtonFamily.SECONDARY)
    val widest = remember(measurer, style, myMusics, compose) {
        listOf(myMusics, compose).map { measurer.widestWord(it, style) }
    }
    SideBySideOrStacked(
        labelWidths = widest,
        padding = ButtonFamily.SECONDARY.sidePadding,
        gap = BloxSpaces.space3,
        modifier = modifier,
    ) { index, sizing ->
        if (index == 0) {
            Tile(BloxIcon.LIST, myMusics, onMyMusics, sizing)
        } else {
            Tile(BloxIcon.COMPOSE, compose, onCompose, sizing)
        }
    }
}

/**
 * A tile: a secondary button in `radius-md`, [icon] above its [label], decorative; the label on as many lines as it
 * needs at its size, shrunk only rather than cut in the middle of a word. TalkBack reads the label, as a button.
 */
@Composable
private fun Tile(
    icon: BloxIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BubbleButton(family = ButtonFamily.SECONDARY, onClick = onClick, modifier = modifier, shape = BloxTheme.shapes.md) {
        Column(
            modifier = Modifier.padding(vertical = BloxSpaces.space3),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BloxIcon(icon, Modifier.testTag(HOME_ICON_TAG))
            val textStyle = LocalTextStyle.current
            Text(text = label, textAlign = TextAlign.Center, autoSize = shrinkToWholeWords(textStyle.fontSize))
        }
    }
}
