package ch.magori.blox.feature.musics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.ScoreId

/** What the player does on « Mes musiques »: the screen hands each action over, and decides nothing. */
@Stable
internal interface MyMusicsActions {
    /** The back button: home. */
    fun onBack()

    /** *Écouter*, or *Arrêter*, the music of [id]. */
    fun onListen(id: ScoreId.Personal)

    /** *Utiliser*, or *En jeu*, on the music of [id]; or its item in the menu. */
    fun onUse(id: ScoreId.Personal)

    /** The name of the music of [id] touched, or *Modifier* in its menu: the music opens in the editor. */
    fun onEdit(id: ScoreId.Personal)

    /** *Renommer*, in the menu of the music of [id]. */
    fun onRename(id: ScoreId.Personal)

    /** Why the music being renamed cannot be called [name]; null when it can. */
    fun renameProblem(name: String): NameProblem?

    /** The dialog *Renommer* confirmed with [name]. */
    fun onRenamed(name: String)

    /** *Supprimer*, in the menu of the music of [id]. */
    fun onDelete(id: ScoreId.Personal)

    /** *Supprimer*, in the confirmation. */
    fun onDeleteConfirmed()

    /** The dialog open cancelled. */
    fun onDialogDismissed()

    /** « Annuler », after a deletion. */
    fun onUndo()

    /** *Fermer le message* of the deletion. */
    fun onUndoDismissed()

    /** *Au hasard*. */
    fun onRandom()

    /** *Nouvelle*. */
    fun onNew()
}

/**
 * « Mes musiques » (V7, V10): the back button and, at the top, the music played during the games; a card per saved
 * music, to listen to it, choose it for the games, rename or delete it from its menu « … »; the message that undoes a
 * deletion; *Au hasard* and *Nouvelle*, which open the editor.
 */
@Composable
internal fun MyMusicsScreen(
    uiState: MyMusicsUiState,
    actions: MyMusicsActions,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.page)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The header stays in place, as in the mockup: the back button is always at hand.
        Header(gameMusic = uiState.gameMusic, onBack = actions::onBack)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.musics.isEmpty()) {
                item(key = EMPTY_KEY) {
                    Text(
                        text = stringResource(R.string.feature_musics_empty),
                        color = palette.label,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
                    )
                }
            }
            items(uiState.musics, key = {
                "$MUSIC_KEY${it.id.key}"
            }) { card -> MusicCardRow(card = card, actions = actions) }
        }
        uiState.deleted?.let { name ->
            UndoMessage(name = name, onUndo = actions::onUndo, onClose = actions::onUndoDismissed)
        }
        BottomButtons(onRandom = actions::onRandom, onNew = actions::onNew)
    }
    when (val dialog = uiState.dialog) {
        is MyMusicsDialog.Rename -> RenameDialog(
            dialog = dialog,
            problemOf = actions::renameProblem,
            onRenamed = actions::onRenamed,
            onDismiss = actions::onDialogDismissed,
        )

        is MyMusicsDialog.ConfirmDelete -> DeleteDialog(
            dialog = dialog,
            onConfirm = actions::onDeleteConfirmed,
            onDismiss = actions::onDialogDismissed,
        )

        null -> Unit
    }
}

/** The back button, the title, and the music played during the games. */
@Composable
private fun Header(
    gameMusic: String?,
    onBack: () -> Unit,
) {
    val palette = BloxTheme.palette
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlyphButton(
            description = stringResource(R.string.feature_musics_back),
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // On one line, shrunk if needed: the pixel font would cut « musiques » otherwise.
            SingleLineText(
                text = stringResource(R.string.feature_musics_title),
                color = palette.ink,
                style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleLarge),
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(
                    R.string.feature_musics_game_music,
                    gameMusic ?: stringResource(R.string.feature_musics_game_music_home),
                ),
                color = palette.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The card of a music, on a single row as high as its buttons (V7, V10): listen; its name and its info line, which
 * open the music in the editor; *Utiliser* or *En jeu*, a square beside them; and its menu « … ».
 */
@Composable
private fun MusicCardRow(
    card: MusicCard,
    actions: MyMusicsActions,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.panel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val listening = card.listening
        val listenDescription =
            if (listening !=
                null
            ) {
                R.string.feature_musics_stop_description
            } else {
                R.string.feature_musics_listen_description
            }
        GlyphButton(
            description = stringResource(listenDescription, card.name),
            onClick = { actions.onListen(card.id) },
            background = palette.page,
            glyph = { ink -> if (listening != null) Glyphs.stop(this, ink) else Glyphs.play(this, ink) },
        )
        // Touching the name, or its info line, opens the music in the editor; TalkBack says so. 48 dp high at least.
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = BloxSizes.minTouchTarget)
                .clickable(
                    onClickLabel = stringResource(R.string.feature_musics_menu_edit),
                    role = Role.Button,
                    onClick = { actions.onEdit(card.id) },
                ),
        ) {
            Text(
                text = card.name,
                color = palette.ink,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CardInfo(card = card, listening = listening)
        }
        UseSwitch(name = card.name, inGame = card.inGame, onClick = { actions.onUse(card.id) })
        CardMenu(card = card, actions = actions)
    }
}

/**
 * The line under the name of a card, smaller, on one line, cut short if needed: its style, its key and its length,
 * « Joyeux · sol majeur · 0:16 »; or, while it is [listening] to, the bar played, in the ink as the mockup shows it.
 * TalkBack says it in words instead ([cardInfoDescription]): « Joyeux, sol majeur, 16 secondes », not « 0:16 » nor
 * « · ».
 */
@Composable
private fun CardInfo(
    card: MusicCard,
    listening: Listening?,
) {
    val palette = BloxTheme.palette
    val style = stringResource(categoryLabel(card.category))
    val description = cardInfoDescription(
        style = style,
        key = card.key,
        spokenLength = card.spokenLength,
        listening = listening,
    )
    // Its text left to the description alone: in the button that opens the editor, TalkBack would read « · » and
    // « 0:24 » too. Its layout stays exposed, as the one of a text, for the services that look for it.
    val laidOut = remember { LaidOutText() }
    Text(
        text = if (listening != null) {
            stringResource(R.string.feature_musics_listening, listening.bar, listening.bars)
        } else {
            stringResource(R.string.feature_musics_info, style, card.key, card.length)
        },
        color = if (listening != null) palette.ink else palette.label,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { laidOut.result = it },
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = description
            getTextLayoutResult { results -> laidOut.result?.let(results::add) ?: false }
        },
    )
}

/** The last layout of a text, kept without recomposing anything. */
private class LaidOutText {
    var result: TextLayoutResult? = null
}

/**
 * *Utiliser*, a note framed; or *En jeu*, a tick on the accent, as a selected choice of the home screen: a square of
 * 48 dp. A switch for TalkBack: « Thème du soir pendant les parties », on or off.
 */
@Composable
private fun UseSwitch(
    name: String,
    inGame: Boolean,
    onClick: () -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.button
    val ink = if (inGame) palette.accentInk else palette.ink
    val description = stringResource(R.string.feature_musics_use_description, name)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(BloxSizes.minTouchTarget)
            .clip(shape)
            .background(if (inGame) palette.accent else palette.panel, shape)
            .border(BloxBorders.frame, if (inGame) palette.accent else palette.panelBorder, shape)
            .toggleable(value = inGame, role = Role.Switch) { onClick() }
            .semantics { contentDescription = description },
    ) {
        Spacer(
            modifier = Modifier.size(GlyphSize).drawBehind {
                if (inGame) tick(ink) else note(ink)
            },
        )
    }
}

/** A note of music: a head, its stem and its flag. Drawn here until the icons of the project come (P5.54). */
private fun DrawScope.note(color: Color) {
    val stroke = size.width * GLYPH_STROKE
    drawOval(
        color,
        topLeft = Offset(size.width * NOTE_HEAD_LEFT, size.height * 0.62f),
        size = Size(size.width * (NOTE_HEAD_RIGHT - NOTE_HEAD_LEFT), size.height * 0.24f),
    )
    // The stem rises from the right edge of the head, inside it; its square ends match those of the tick.
    val stem = size.width * NOTE_HEAD_RIGHT - stroke / 2
    val top = Offset(stem, size.height * 0.15f)
    drawLine(color, Offset(stem, size.height * 0.74f), top, stroke, cap = StrokeCap.Square)
    drawLine(color, top, Offset(size.width * 0.8f, size.height * 0.32f), stroke, cap = StrokeCap.Square)
}

/** The left and right edges of the head of the note, in parts of its box. */
private const val NOTE_HEAD_LEFT = 0.2f
private const val NOTE_HEAD_RIGHT = 0.54f

/** A tick: on. Drawn here until the icons of the project come (P5.54). */
private fun DrawScope.tick(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.52f)
        lineTo(size.width * 0.42f, size.height * 0.74f)
        lineTo(size.width * 0.8f, size.height * 0.28f)
    }
    drawPath(
        path,
        color,
        style = Stroke(width = size.width * GLYPH_STROKE, cap = StrokeCap.Square, join = StrokeJoin.Miter),
    )
}

/** The box the glyphs of a card are drawn in, and the width of their strokes, as those of the other buttons. */
private val GlyphSize = 24.dp
private const val GLYPH_STROKE = 0.12f

/** The menu « … » of a card: *Modifier*, *Renommer*, *La jouer* or *Ne plus la jouer pendant les parties*, *Supprimer*. */
@Composable
private fun CardMenu(
    card: MusicCard,
    actions: MyMusicsActions,
) {
    val palette = BloxTheme.palette
    Box {
        var expanded by remember { mutableStateOf(false) }
        GlyphButton(
            description = stringResource(R.string.feature_musics_menu_description, card.name),
            onClick = { expanded = true },
            glyph = { Glyphs.more(this, it) },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = palette.panel,
        ) {
            MenuItem(R.string.feature_musics_menu_edit, palette.ink) {
                expanded = false
                actions.onEdit(card.id)
            }
            MenuItem(R.string.feature_musics_menu_rename, palette.ink) {
                expanded = false
                actions.onRename(card.id)
            }
            MenuItem(
                if (card.inGame) R.string.feature_musics_menu_stop_using else R.string.feature_musics_menu_use,
                palette.ink,
            ) {
                expanded = false
                actions.onUse(card.id)
            }
            MenuItem(R.string.feature_musics_menu_delete, palette.danger) {
                expanded = false
                actions.onDelete(card.id)
            }
        }
    }
}

@Composable
private fun MenuItem(
    text: Int,
    color: Color,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(text), color = color, style = MaterialTheme.typography.labelLarge) },
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

/** « « X » est supprimée. », with « Annuler » and its cross: it stays until the next action. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UndoMessage(
    name: String,
    onUndo: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = BloxTheme.palette
    // The actions beside the message, or under it when they do not fit: the message is never cut.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.ink, BloxTheme.shapes.panel)
            .padding(start = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feature_musics_deleted, name),
            color = palette.page,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            TextButtonBox(
                text = stringResource(R.string.feature_musics_undo),
                background = palette.ink,
                ink = palette.page,
                frame = palette.ink,
                onClick = onUndo,
            )
            GlyphButton(
                description = stringResource(R.string.feature_musics_close_message),
                onClick = onClose,
                ink = palette.page,
                glyph = { Glyphs.close(this, it) },
            )
        }
    }
}

/** *Au hasard*, framed as a panel, and *Nouvelle*, in the accent, side by side. */
@Composable
private fun BottomButtons(
    onRandom: () -> Unit,
    onNew: () -> Unit,
) {
    val palette = BloxTheme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButtonBox(
            text = stringResource(R.string.feature_musics_random),
            background = palette.panel,
            ink = palette.ink,
            frame = palette.panelBorder,
            onClick = onRandom,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        )
        TextButtonBox(
            text = stringResource(R.string.feature_musics_new),
            background = palette.accent,
            ink = palette.accentInk,
            frame = palette.accent,
            onClick = onNew,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        )
    }
}

@Composable
private fun MyMusicsPreview(
    kind: BloxThemeKind,
    uiState: MyMusicsUiState = previewState,
) {
    BloxTheme(kind = kind) {
        MyMusicsScreen(uiState = uiState, actions = NoActions)
    }
}

/**
 * Three musics, the first played during the games, the second listened to, its name too long for its card, the third
 * written by hand.
 */
private val previewState = MyMusicsUiState(
    musics = listOf(
        MusicCard(
            ScoreId.Personal("1"),
            "Thème du soir",
            Category.CALM,
            key = "fa majeur",
            length = "0:24",
            spokenLength = "24 secondes",
            inGame = true,
        ),
        MusicCard(
            ScoreId.Personal("2"),
            "Ballades pour une longue soirée en hiver",
            Category.RETRO,
            key = "fa dièse mixolydien",
            length = "0:12",
            spokenLength = "12 secondes",
            inGame = false,
            listening = Listening(bar = 3, bars = 8),
        ),
        MusicCard(
            ScoreId.Personal("3"),
            "Ma marche",
            category = null,
            key = "la mineur",
            length = "0:15",
            spokenLength = "15 secondes",
            inGame = false,
        ),
    ),
    gameMusic = "Thème du soir",
    deleted = "Joyeux 2",
)

/** The actions of the previews: none. */
private object NoActions : MyMusicsActions {
    override fun onBack() = Unit

    override fun onListen(id: ScoreId.Personal) = Unit

    override fun onUse(id: ScoreId.Personal) = Unit

    override fun onEdit(id: ScoreId.Personal) = Unit

    override fun onRename(id: ScoreId.Personal) = Unit

    override fun renameProblem(name: String): NameProblem? = null

    override fun onRenamed(name: String) = Unit

    override fun onDelete(id: ScoreId.Personal) = Unit

    override fun onDeleteConfirmed() = Unit

    override fun onDialogDismissed() = Unit

    override fun onUndo() = Unit

    override fun onUndoDismissed() = Unit

    override fun onRandom() = Unit

    override fun onNew() = Unit
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsOriginalPreview() {
    MyMusicsPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsColorfulPreview() {
    MyMusicsPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun MyMusicsOriginalLargeFontPreview() {
    MyMusicsPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun MyMusicsColorfulLargeFontPreview() {
    MyMusicsPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsEmptyPreview() {
    MyMusicsPreview(BloxThemeKind.COLORFUL, MyMusicsUiState(musics = emptyList(), gameMusic = null))
}

/** The keys of the items of the list: a key of music never takes the one of the empty list. */
private const val EMPTY_KEY = "empty"
private const val MUSIC_KEY = "music:"
