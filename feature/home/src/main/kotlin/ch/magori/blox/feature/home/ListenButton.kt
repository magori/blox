package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The tag of the glyph of the listen button, decorative, read by the tests only. */
internal const val LISTEN_GLYPH_TAG = "home_listen_glyph"

/** The glyph before the label of the listen button: a triangle to listen, a square to stop. */
internal enum class ListenGlyph { PLAY, STOP }

/**
 * What the listen button of a music shows, [label] after its [glyph], and what TalkBack reads, [description]: the
 * button alone says « Écouter », TalkBack says which music.
 */
internal data class ListenTexts(
    @param:StringRes val label: Int,
    @param:StringRes val description: Int,
    val glyph: ListenGlyph,
)

/** The texts of the listen button of a music, [previewed] or not: listen to it, or stop it. */
internal fun listenTexts(previewed: Boolean): ListenTexts = if (previewed) {
    ListenTexts(
        label = R.string.feature_home_stop,
        description = R.string.feature_home_stop_description,
        glyph = ListenGlyph.STOP,
    )
} else {
    ListenTexts(
        label = R.string.feature_home_listen,
        description = R.string.feature_home_listen_description,
        glyph = ListenGlyph.PLAY,
    )
}

/**
 * Listens to the music named [musicName] before playing, or stops it if [previewed]. The glyph before the label is
 * decorative: TalkBack reads « Écouter Menuet », nothing more.
 */
@Composable
internal fun ListenButton(
    musicName: String,
    previewed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = listenTexts(previewed)
    val description = stringResource(texts.description, musicName)
    val ink = BloxTheme.palette.label
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = description },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(GLYPH_GAP), verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .testTag(LISTEN_GLYPH_TAG)
                    .size(GLYPH_SIZE)
                    .drawBehind {
                        when (texts.glyph) {
                            ListenGlyph.PLAY -> Glyphs.play(this, ink)
                            ListenGlyph.STOP -> Glyphs.stop(this, ink)
                        }
                    },
            )
            Text(text = stringResource(texts.label), color = ink, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The glyph beside the label, as large as the icons beside a label on the mockup. */
private val GLYPH_SIZE = 16.dp

/** The room between the glyph and the label. */
private val GLYPH_GAP = 6.dp
