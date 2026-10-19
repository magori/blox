package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import ch.magori.blox.core.designsystem.theme.BloxAlphas
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.feature.composer.grid.trackColors
import ch.magori.blox.feature.composer.model.BARS
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.ui.ChoiceButton
import ch.magori.blox.feature.composer.ui.ComposerSizes
import ch.magori.blox.feature.composer.ui.ToolButton

/** The tabs *Mélodie* and *Basse*: the open [track], in the colours of its tab ([trackColors]). */
@Composable
internal fun TrackTabs(
    track: Track,
    onTrackChosen: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
    ) {
        Track.entries.forEach { tab ->
            val colors = trackColors(palette, tab)
            ChoiceButton(
                text = stringResource(tab.label),
                selected = tab == track,
                onClick = { onTrackChosen(tab) },
                role = Role.Tab,
                selectedColor = colors.tab,
                selectedInk = colors.tabInk,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The toolbar: *Poser* / *Allonger*, the [mode] of the touches, then *Annuler*, *Copier la mesure* and *Effacer la
 * mesure*, each disabled when it has no effect.
 */
@Composable
internal fun ComposerToolbar(
    mode: EditMode,
    canUndo: Boolean,
    canCopyBar: Boolean,
    canClearBar: Boolean,
    onModeChosen: (EditMode) -> Unit,
    onUndo: () -> Unit,
    onCopyBar: () -> Unit,
    onClearBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = EditMode.entries.associateWith { stringResource(it.label) }
    val undo = stringResource(R.string.feature_composer_tool_undo)
    val copy = stringResource(R.string.feature_composer_tool_copy)
    val clear = stringResource(R.string.feature_composer_tool_clear)
    // Each button as wide as its label needs, with a character to spare: equal widths cut the longest labels of the
    // pixel font on 360 dp, widths of the label alone leave « Poser » under 48 dp.
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap)) {
        Row(
            modifier = Modifier.weight(modes.values.map(::widthOf).sum()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
        ) {
            modes.forEach { (choice, text) ->
                ChoiceButton(
                    text = text,
                    selected = choice == mode,
                    onClick = { onModeChosen(choice) },
                    role = Role.RadioButton,
                    modifier = Modifier.weight(widthOf(text)),
                )
            }
        }
        ToolButton(
            text = undo,
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.weight(widthOf(undo)),
        )
        ToolButton(
            text = copy,
            description = stringResource(R.string.feature_composer_tool_copy_description),
            onClick = onCopyBar,
            enabled = canCopyBar,
            modifier = Modifier.weight(widthOf(copy)),
        )
        ToolButton(
            text = clear,
            description = stringResource(R.string.feature_composer_tool_clear_description),
            onClick = onClearBar,
            enabled = canClearBar,
            modifier = Modifier.weight(widthOf(clear)),
        )
    }
}

/**
 * The eight bar buttons, the selected [bar], counted from 0, filled; the bar played while the music is listened to,
 * [playedBar], marked apart, and read « en lecture ». They are 48 dp high but narrower than 48 dp on a phone, as the
 * start levels of the home screen: eight of them do not fit otherwise on 360 dp.
 */
@Composable
internal fun BarButtons(
    bar: Int,
    playedBar: Int?,
    onBarChosen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val played = stringResource(R.string.feature_composer_bar_played)
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
    ) {
        (0 until BARS).forEach { index ->
            ChoiceButton(
                text = (index + 1).toString(),
                description = stringResource(R.string.feature_composer_bar, index + 1),
                selected = index == bar,
                onClick = { onBarChosen(index) },
                role = Role.Tab,
                mark = played.takeIf { index == playedBar },
                modifier = Modifier.weight(1f).testTag("composer_bar"),
            )
        }
    }
}

/**
 * *Écouter*, a round button that plays the music, or *Arrêter* while it is [listening]; disabled while there is nothing
 * to hear. Its glyph, a triangle or a square, says which; TalkBack reads its name.
 */
@Composable
internal fun ListenButton(
    listening: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val description =
        stringResource(if (listening) R.string.feature_composer_stop else R.string.feature_composer_listen)
    Spacer(
        modifier = modifier
            .size(BloxSizes.minTouchTarget)
            .alpha(if (enabled) 1f else BloxAlphas.DISABLED)
            .clip(CircleShape)
            .background(palette.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .drawBehind { if (listening) drawStop(palette.accentInk) else drawPlay(palette.accentInk) },
    )
}

/** A square, in the middle of the button. */
private fun DrawScope.drawStop(color: Color) {
    val side = size.minDimension * GLYPH_PART * STOP_PART
    drawRect(color, topLeft = Offset((size.width - side) / 2, (size.height - side) / 2), size = Size(side, side))
}

/** A triangle pointing right, in the middle of the button, nudged right so that it looks centred. */
private fun DrawScope.drawPlay(color: Color) {
    val side = size.minDimension * GLYPH_PART
    val left = (size.width - side) / 2 + side * PLAY_NUDGE
    val top = (size.height - side) / 2
    val path = Path().apply {
        moveTo(left, top)
        lineTo(left + side * PLAY_WIDTH, size.height / 2)
        lineTo(left, top + side)
        close()
    }
    drawPath(path, color)
}

/** The glyphs of *Écouter* fill this part of the button. */
private const val GLYPH_PART = 0.42f

/** The square of *Arrêter*, smaller than the triangle, which looks smaller than its box. */
private const val STOP_PART = 0.8f

/** The triangle of *Écouter*: as wide as this part of its height, nudged right by this part of its height. */
private const val PLAY_WIDTH = 0.9f
private const val PLAY_NUDGE = 0.08f

/** The tempo of the music, [bpm] beats per minute, between its buttons « − » and « + », disabled at the ends. */
@Composable
internal fun TempoPanel(
    bpm: Int,
    canSlowDown: Boolean,
    canSpeedUp: Boolean,
    onSlower: () -> Unit,
    onFaster: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(
            text = stringResource(R.string.feature_composer_slower_glyph),
            description = stringResource(R.string.feature_composer_slower),
            onClick = onSlower,
            enabled = canSlowDown,
            modifier = Modifier.width(BloxSizes.minTouchTarget),
        )
        Text(
            text = pluralStringResource(R.plurals.feature_composer_tempo, bpm, bpm),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            // The new tempo is read out after a touch on « − » or « + ».
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        ToolButton(
            text = stringResource(R.string.feature_composer_faster_glyph),
            description = stringResource(R.string.feature_composer_faster),
            onClick = onFaster,
            enabled = canSpeedUp,
            modifier = Modifier.width(BloxSizes.minTouchTarget),
        )
    }
}

@get:StringRes
private val Track.label: Int
    get() = when (this) {
        Track.MELODY -> R.string.feature_composer_tab_melody
        Track.BASS -> R.string.feature_composer_tab_bass
    }

@get:StringRes
private val EditMode.label: Int
    get() = when (this) {
        EditMode.PLACE -> R.string.feature_composer_tool_place
        EditMode.LENGTHEN -> R.string.feature_composer_tool_lengthen
    }

/** The share of the toolbar given to a button labelled [label]: its characters, and one to spare. */
private fun widthOf(label: String): Float = label.length + 1f
