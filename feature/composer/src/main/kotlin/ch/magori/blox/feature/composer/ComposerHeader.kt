package ch.magori.blox.feature.composer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.feature.composer.model.SaveStatus
import ch.magori.blox.feature.composer.ui.ComposerSizes

/** The glyphs of the header, drawn: this part of their 24 dp box. */
private val GlyphSize = 24.dp
private const val GLYPH_STROKE = 0.12f
private const val DOT_RADIUS = 0.09f

/**
 * The header of the editor: the back button, the title with its info line ([status] of the music), and the menu
 * « … », which only holds *Montrer l'autre piste en filigrane* for now.
 */
@Composable
internal fun ComposerHeader(
    status: SaveStatus,
    showOtherTrack: Boolean,
    onBack: () -> Unit,
    onOtherTrackShown: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(description = stringResource(R.string.feature_composer_back), onClick = onBack) { drawBack(it) }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.feature_composer_title_new),
                color = palette.ink,
                style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleSmall),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            statusText(status)?.let {
                Text(text = it, color = palette.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        Box {
            var expanded by remember { mutableStateOf(false) }
            GlyphButton(description = stringResource(R.string.feature_composer_menu), onClick = { expanded = true }) {
                drawDots(it)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_composer_menu_other_track)) },
                    trailingIcon = {
                        Checkbox(
                            checked = showOtherTrack,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = palette.accent,
                                checkmarkColor = palette.accentInk,
                                uncheckedColor = palette.ink,
                            ),
                        )
                    },
                    onClick = {
                        onOtherTrackShown(!showOtherTrack)
                        expanded = false
                    },
                    // Read by TalkBack as the check box it shows, ticked or not.
                    modifier = Modifier.semantics {
                        role = Role.Checkbox
                        toggleableState = ToggleableState(showOtherTrack)
                    },
                )
            }
        }
    }
}

/** The info line: « non enregistrée » for a new music, « modifiée » for a saved one changed, nothing once saved. */
@Composable
private fun statusText(status: SaveStatus): String? = when (status) {
    SaveStatus.NOT_SAVED -> stringResource(R.string.feature_composer_status_not_saved)
    SaveStatus.MODIFIED -> stringResource(R.string.feature_composer_status_modified)
    SaveStatus.SAVED -> null
}

/** A button of the header, its glyph drawn by [glyph] in `ink`, on a touch target of 48 dp. */
@Composable
private fun GlyphButton(
    description: String,
    onClick: () -> Unit,
    glyph: DrawScope.(ink: Color) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .sizeIn(minWidth = BloxSizes.minTouchTarget, minHeight = BloxSizes.minTouchTarget)
            .clip(BloxTheme.shapes.button)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        val ink = BloxTheme.palette.ink
        Spacer(modifier = Modifier.size(GlyphSize).drawBehind { glyph(ink) })
    }
}

/** A chevron pointing left. */
private fun DrawScope.drawBack(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.62f, size.height * 0.2f)
        lineTo(size.width * 0.3f, size.height * 0.5f)
        lineTo(size.width * 0.62f, size.height * 0.8f)
    }
    drawPath(
        path,
        color,
        style = Stroke(width = size.width * GLYPH_STROKE, cap = StrokeCap.Square, join = StrokeJoin.Miter),
    )
}

/** Three dots in a row: « … ». */
private fun DrawScope.drawDots(color: Color) {
    listOf(0.2f, 0.5f, 0.8f).forEach { x ->
        drawCircle(color, radius = size.width * DOT_RADIUS, center = Offset(size.width * x, size.height / 2))
    }
}
