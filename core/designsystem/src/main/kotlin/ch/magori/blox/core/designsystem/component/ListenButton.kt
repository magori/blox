package ch.magori.blox.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSizes

/**
 * Écouter, a disc of the family Icône, [size] wide and high (48 dp, 56 in the editor): a triangle, or a square while
 * the music is [playing], when the button keeps its pressed fill (`active`). TalkBack reads « Écouter » or « Arrêter »
 * with the name of the music, [musicName], or the verb alone for a music without a name; the icon adds nothing.
 */
@Composable
fun ListenButton(
    playing: Boolean,
    musicName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = BloxSizes.minTouchTarget,
    enabled: Boolean = true,
) {
    val face = listenFace(playing)
    val description = if (musicName == null) stringResource(face.verb) else stringResource(face.named, musicName)
    GlyphButton(
        icon = face.icon,
        description = description,
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        active = face.active,
    )
}

/**
 * What the listen button shows, [icon], and what TalkBack reads of it: [verb] alone for a music without a name,
 * [named] with its name otherwise; [active], it keeps its pressed fill while the music plays.
 */
internal data class ListenFace(
    val icon: BloxIcon,
    @param:StringRes val verb: Int,
    @param:StringRes val named: Int,
    val active: Boolean,
)

/** The face of the listen button of a music [playing] or not: listen to it, or stop it. */
internal fun listenFace(playing: Boolean): ListenFace = if (playing) {
    ListenFace(BloxIcon.STOP, R.string.core_designsystem_stop, R.string.core_designsystem_stop_named, active = true)
} else {
    ListenFace(
        BloxIcon.PLAY,
        R.string.core_designsystem_listen,
        R.string.core_designsystem_listen_named,
        active = false,
    )
}
