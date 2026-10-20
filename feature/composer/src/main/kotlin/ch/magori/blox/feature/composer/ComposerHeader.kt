package ch.magori.blox.feature.composer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
 * The header of the editor: the back button, the title, the [name] of the music (« Nouvelle musique » until it is
 * named), to touch to rename it, with its pencil, and its info line, its [category] and its [status]; and the menu
 * « … »: *Renommer*, *Suivre la lecture*, *Montrer l'autre piste en filigrane* and, for a saved music ([canDelete]),
 * *Supprimer*.
 */
@Composable
internal fun ComposerHeader(
    name: String?,
    category: String?,
    status: SaveStatus,
    canDelete: Boolean,
    showOtherTrack: Boolean,
    followPlayback: Boolean,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOtherTrackShown: (Boolean) -> Unit,
    onFollowPlayback: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(description = stringResource(R.string.feature_composer_back), onClick = onBack) { drawBack(it) }
        val title = name ?: stringResource(R.string.feature_composer_title_new)
        val renameTitle = stringResource(R.string.feature_composer_title_rename, title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = BloxSizes.minTouchTarget)
                .clip(BloxTheme.shapes.button)
                .clickable(role = Role.Button, onClick = onRename)
                .semantics(mergeDescendants = true) { contentDescription = renameTitle },
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    color = palette.ink,
                    style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleSmall),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                infoText(category, status)?.let {
                    Text(text = it, color = palette.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.padding(start = 4.dp).size(PencilSize).drawBehind { drawPencil(palette.ink) })
        }
        Box {
            var expanded by remember { mutableStateOf(false) }
            GlyphButton(description = stringResource(R.string.feature_composer_menu), onClick = { expanded = true }) {
                drawDots(it)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_composer_menu_rename)) },
                    onClick = {
                        expanded = false
                        onRename()
                    },
                )
                CheckItem(
                    text = stringResource(R.string.feature_composer_menu_follow),
                    checked = followPlayback,
                    onChecked = {
                        onFollowPlayback(it)
                        expanded = false
                    },
                )
                CheckItem(
                    text = stringResource(R.string.feature_composer_menu_other_track),
                    checked = showOtherTrack,
                    onChecked = {
                        onOtherTrackShown(it)
                        expanded = false
                    },
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.feature_composer_menu_delete), color = palette.danger) },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

/** An item of the menu « … » that is [checked] or not, with its check box; touching it calls [onChecked] with the other. */
@Composable
private fun CheckItem(
    text: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    val palette = BloxTheme.palette
    DropdownMenuItem(
        text = { Text(text) },
        trailingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    checkmarkColor = palette.accentInk,
                    uncheckedColor = palette.ink,
                ),
            )
        },
        onClick = { onChecked(!checked) },
        // Read by TalkBack as the check box it shows, ticked or not.
        modifier = Modifier.semantics {
            role = Role.Checkbox
            toggleableState = ToggleableState(checked)
        },
    )
}

/**
 * The info line: the [category], if any, then « non enregistrée » for a new music, « modifiée » for a saved one
 * changed, nothing once saved; null when it says nothing.
 */
@Composable
private fun infoText(
    category: String?,
    status: SaveStatus,
): String? {
    val saving = when (status) {
        SaveStatus.NOT_SAVED -> stringResource(R.string.feature_composer_status_not_saved)
        SaveStatus.MODIFIED -> stringResource(R.string.feature_composer_status_modified)
        SaveStatus.SAVED -> null
    }
    return when {
        category != null && saving != null -> stringResource(R.string.feature_composer_info, category, saving)
        else -> category ?: saving
    }
}

/** A pencil, slanted, its tip at the bottom left: the title renames the music. */
private fun DrawScope.drawPencil(color: Color) {
    val stroke = size.width * GLYPH_STROKE
    drawLine(
        color,
        start = Offset(size.width * 0.25f, size.height * 0.75f),
        end = Offset(size.width * 0.8f, size.height * 0.2f),
        strokeWidth = stroke * 2,
    )
    val tip = Path().apply {
        moveTo(size.width * 0.12f, size.height * 0.88f)
        lineTo(size.width * 0.16f, size.height * 0.62f)
        lineTo(size.width * 0.38f, size.height * 0.84f)
        close()
    }
    drawPath(tip, color)
}

/** The pencil after the title. */
private val PencilSize = 16.dp

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
